package com.lumora.sms.service.impl;

import com.lumora.sms.dto.StudentImportResult;
import com.lumora.sms.dto.StudentImportResult.RowProblem;
import com.lumora.sms.entity.*;
import com.lumora.sms.enums.Gender;
import com.lumora.sms.exception.BusinessRuleViolationException;
import com.lumora.sms.repository.*;
import com.lumora.sms.service.StudentImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

/** Reads, checks and saves a learner spreadsheet in a single transaction. */
@Service
@RequiredArgsConstructor
public class StudentImportServiceImpl implements StudentImportService {

    static final int MAX_ROWS = 5000;

    /** Headings in the template, in order. */
    static final List<String> COLUMNS = List.of(
            "admission_number", "first_name", "middle_name", "last_name", "gender", "date_of_birth",
            "admission_date", "form", "stream", "county", "guardian_name", "guardian_phone",
            "guardian_relationship");

    /** The many ways other systems and people write each heading. */
    static final Map<String, String> ALIASES = aliases();

    private final StudentRepository students;
    private final SchoolRepository schools;
    private final AcademicYearRepository years;
    private final ClassStreamRepository streams;
    private final StudentEnrollmentRepository enrollments;
    private final ParentRepository parents;
    private final StudentGuardianRepository guardians;

    private static Map<String, String> aliases() {
        Map<String, String> m = new HashMap<>();
        put(m, "admission_number", "admissionnumber", "admissionno", "admno", "adm", "admission", "regno",
                "registrationnumber", "studentnumber", "studentno", "upi");
        put(m, "first_name", "firstname", "first", "givenname", "fname");
        put(m, "middle_name", "middlename", "middle", "othername", "othernames");
        put(m, "last_name", "lastname", "last", "surname", "familyname", "lname");
        put(m, "full_name", "name", "fullname", "studentname", "learnername", "names");
        put(m, "gender", "gender", "sex");
        put(m, "date_of_birth", "dateofbirth", "dob", "birthdate", "birthday", "dateborn");
        put(m, "admission_date", "admissiondate", "dateofadmission", "dateadmitted", "admitted", "joined",
                "datejoined");
        put(m, "form", "form", "class", "grade", "level", "formclass");
        put(m, "stream", "stream", "streamname", "section");
        put(m, "county", "county", "homecounty");
        put(m, "guardian_name", "guardianname", "guardian", "parentname", "parent", "parentguardian",
                "parentguardianname", "contactname");
        put(m, "guardian_phone", "guardianphone", "parentphone", "phone", "phonenumber", "telephone",
                "mobile", "contact", "contactphone", "guardianmobile", "parentmobile", "guardiantel");
        put(m, "guardian_relationship", "guardianrelationship", "relationship", "relation", "relationtostudent");
        return Map.copyOf(m);
    }

    private static void put(Map<String, String> m, String column, String... names) {
        for (String name : names) {
            m.put(name, column);
        }
    }

    @Override
    public String templateCsv() {
        return String.join(",", COLUMNS) + "\r\n"
                + "ADM/2027/001,Brian,Kiprotich,Kamau,Male,14/03/2012,08/01/2027,Form 1,East,Nakuru,"
                + "Mary Kamau,0712345678,Mother\r\n"
                + "ADM/2027/002,Faith,,Achieng,Female,2011-09-30,2027-01-08,Form 2,West,Kisumu,"
                + "Joseph Achieng,+254722000111,Father\r\n";
    }

    /** One checked row, ready to save. */
    private record Learner(int row, String admissionNumber, String firstName, String middleName,
                           String lastName, Gender gender, LocalDate dateOfBirth, LocalDate admissionDate,
                           ClassStream stream, String county, String guardianName, String guardianPhone,
                           String relationship) {
    }

    @Override
    @Transactional
    public StudentImportResult importCsv(byte[] file, boolean skipExisting) {
        CsvTable table = CsvTable.parse(file, ALIASES, MAX_ROWS);
        Set<String> found = new HashSet<>(table.headings());
        List<String> missing = new ArrayList<>();
        if (!found.contains("admission_number")) {
            missing.add("admission number");
        }
        if (!found.contains("full_name") && !(found.contains("first_name") && found.contains("last_name"))) {
            missing.add("first name and last name (or one name column)");
        }
        for (String column : List.of("gender", "date_of_birth", "form", "stream")) {
            if (!found.contains(column)) {
                missing.add(column.replace('_', ' '));
            }
        }
        if (!missing.isEmpty()) {
            throw new BusinessRuleViolationException("The file has no column for: " + String.join(", ", missing)
                    + ". Download the template to see the headings Lumora expects.");
        }
        if (table.rows().isEmpty()) {
            throw new BusinessRuleViolationException("The file has headings but no learners under them.");
        }

        AcademicYear year = years.findFirstByActiveTrue().orElseThrow(() ->
                new BusinessRuleViolationException("Lumora has no current academic year yet."));
        School school = year.getSchool();
        Map<String, ClassStream> streamsByName = new HashMap<>();
        for (ClassStream stream : streams.findAllByOrderBySchoolClassLevelAscNameAsc()) {
            streamsByName.put(stream.getSchoolClass().getLevel() + "|" + CsvTable.key(stream.getName()), stream);
        }

        List<RowProblem> problems = new ArrayList<>();
        List<Learner> learners = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        int skipped = 0;

        for (CsvTable.Row row : table.rows()) {
            List<String> rowProblems = new ArrayList<>();
            String admission = row.get("admission_number").toUpperCase(Locale.ROOT);
            if (admission.isEmpty()) {
                rowProblems.add("The admission number is missing.");
            } else if (admission.length() > 30) {
                rowProblems.add("The admission number is longer than 30 characters.");
            } else if (!seen.add(admission)) {
                rowProblems.add("This admission number appears more than once in the file.");
            } else if (students.existsByAdmissionNumber(admission)
                    || students.findByAdmissionNumber(row.get("admission_number")).isPresent()) {
                if (skipExisting) {
                    skipped++;
                    continue;
                }
                rowProblems.add("A learner with this admission number is already in Lumora.");
            }

            String[] names = names(row);
            if (names[0].isEmpty() || names[2].isEmpty()) {
                rowProblems.add("Both a first name and a last name are needed.");
            }
            for (String name : names) {
                if (name.length() > 80) {
                    rowProblems.add("A name is longer than 80 characters.");
                    break;
                }
            }

            Gender gender = gender(row.get("gender"));
            if (gender == null) {
                rowProblems.add("Gender \"" + row.get("gender") + "\" is not Male or Female.");
            }

            LocalDate born = null;
            try {
                born = date(row.get("date_of_birth"));
                if (born == null) {
                    rowProblems.add("The date of birth is missing.");
                } else if (!born.isBefore(LocalDate.now())) {
                    rowProblems.add("The date of birth is not in the past.");
                }
            } catch (DateTimeParseException e) {
                rowProblems.add("The date of birth \"" + row.get("date_of_birth") + "\" is not a date.");
            }

            LocalDate admitted = null;
            try {
                admitted = date(row.get("admission_date"));
                if (admitted != null && born != null && admitted.isBefore(born)) {
                    rowProblems.add("The admission date is before the date of birth.");
                }
            } catch (DateTimeParseException e) {
                rowProblems.add("The admission date \"" + row.get("admission_date") + "\" is not a date.");
            }

            Integer level = formLevel(row.get("form"));
            ClassStream stream = null;
            if (level == null) {
                rowProblems.add("Form \"" + row.get("form") + "\" is not Form 1, 2, 3 or 4.");
            } else {
                stream = streamsByName.get(level + "|" + CsvTable.key(row.get("stream")));
                if (stream == null) {
                    rowProblems.add("Form " + level + " has no stream called \"" + row.get("stream") + "\".");
                }
            }

            String guardianName = row.get("guardian_name");
            String guardianPhone = row.get("guardian_phone");
            if (guardianName.isEmpty() != guardianPhone.isEmpty()) {
                rowProblems.add("A guardian needs both a name and a phone number.");
            } else if (!guardianPhone.isEmpty() && Phones.normalise(guardianPhone).length() < 9) {
                rowProblems.add("The guardian phone number \"" + guardianPhone + "\" is too short.");
            } else if (guardianPhone.length() > 30) {
                rowProblems.add("The guardian phone number is longer than 30 characters.");
            }

            for (String message : rowProblems) {
                problems.add(new RowProblem(row.number(), admission, message));
            }
            if (rowProblems.isEmpty()) {
                learners.add(new Learner(row.number(), admission, names[0], names[1].isEmpty() ? null : names[1],
                        names[2], gender, born, admitted, stream, emptyToNull(row.get("county")),
                        guardianName, guardianPhone, relationship(row.get("guardian_relationship"))));
            }
        }

        if (!problems.isEmpty()) {
            return new StudentImportResult(table.rows().size(), 0, skipped, 0, problems);
        }

        // Guardians already in Lumora (siblings, earlier imports) are reused by phone number.
        Map<String, Parent> parentsByPhone = new HashMap<>();
        for (Parent parent : parents.findAll()) {
            parentsByPhone.putIfAbsent(Phones.normalise(parent.getPhone()), parent);
        }

        int guardiansAdded = 0;
        for (Learner l : learners) {
            Student student = new Student();
            student.setSchool(school);
            student.setAdmissionNumber(l.admissionNumber());
            student.setFirstName(l.firstName());
            student.setMiddleName(l.middleName());
            student.setLastName(l.lastName());
            student.setGender(l.gender());
            student.setDateOfBirth(l.dateOfBirth());
            student.setAdmissionDate(l.admissionDate());
            student.setCounty(l.county());
            student.setActive(true);
            student = students.save(student);

            StudentEnrollment enrollment = new StudentEnrollment();
            enrollment.setStudent(student);
            enrollment.setAcademicYear(year);
            enrollment.setClassStream(l.stream());
            enrollment.setActive(true);
            enrollments.save(enrollment);

            if (!l.guardianPhone().isEmpty()) {
                String key = Phones.normalise(l.guardianPhone());
                Parent parent = parentsByPhone.get(key);
                if (parent == null) {
                    String[] parts = splitName(l.guardianName());
                    parent = new Parent();
                    parent.setFirstName(parts[0]);
                    parent.setLastName(parts[1].isEmpty() ? l.lastName() : parts[1]);
                    parent.setPhone(l.guardianPhone());
                    parent = parents.save(parent);
                    parentsByPhone.put(key, parent);
                    guardiansAdded++;
                }
                StudentGuardian link = new StudentGuardian();
                link.setStudent(student);
                link.setParent(parent);
                link.setRelationship(l.relationship());
                link.setPrimaryContact(true);
                guardians.save(link);
            }
        }
        return new StudentImportResult(table.rows().size(), learners.size(), skipped, guardiansAdded, List.of());
    }

    /** First, middle and last name, from separate columns or from one "name" column. */
    static String[] names(CsvTable.Row row) {
        String first = row.get("first_name");
        String last = row.get("last_name");
        String middle = row.get("middle_name");
        if (first.isEmpty() && last.isEmpty()) {
            String[] parts = row.get("full_name").trim().split("\\s+");
            if (parts.length == 1) {
                return new String[]{parts[0], "", ""};
            }
            first = parts[0];
            last = parts[parts.length - 1];
            middle = String.join(" ", Arrays.copyOfRange(parts, 1, parts.length - 1));
        }
        return new String[]{titleCase(first), titleCase(middle), titleCase(last)};
    }

    /** "KAMAU" and "kamau" both become "Kamau"; "Mary-Anne" is kept as written. */
    static String titleCase(String name) {
        String trimmed = name.trim().replaceAll("\\s+", " ");
        boolean shouting = trimmed.equals(trimmed.toUpperCase(Locale.ROOT));
        boolean whispering = trimmed.equals(trimmed.toLowerCase(Locale.ROOT));
        if (!shouting && !whispering) {
            return trimmed;
        }
        StringBuilder sb = new StringBuilder();
        boolean start = true;
        for (char ch : trimmed.toLowerCase(Locale.ROOT).toCharArray()) {
            sb.append(start ? Character.toUpperCase(ch) : ch);
            start = ch == ' ' || ch == '-' || ch == '\'';
        }
        return sb.toString();
    }

    static String[] splitName(String full) {
        String[] parts = titleCase(full).split(" ");
        if (parts.length == 1) {
            return new String[]{parts[0], ""};
        }
        return new String[]{String.join(" ", Arrays.copyOfRange(parts, 0, parts.length - 1)), parts[parts.length - 1]};
    }

    static Gender gender(String value) {
        return switch (CsvTable.key(value)) {
            case "m", "male", "boy", "b" -> Gender.MALE;
            case "f", "female", "girl", "g" -> Gender.FEMALE;
            default -> null;
        };
    }

    /** "Form 1", "F1", "1", "Form One" and "Grade 1" are all level 1. */
    static Integer formLevel(String value) {
        String key = CsvTable.key(value).replaceFirst("^(form|grade|class|f)", "");
        return switch (key) {
            case "1", "one", "i" -> 1;
            case "2", "two", "ii" -> 2;
            case "3", "three", "iii" -> 3;
            case "4", "four", "iv" -> 4;
            default -> null;
        };
    }

    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
            DateTimeFormatter.ofPattern("uuuu-M-d").withResolverStyle(java.time.format.ResolverStyle.STRICT),
            DateTimeFormatter.ofPattern("d/M/uuuu").withResolverStyle(java.time.format.ResolverStyle.STRICT),
            DateTimeFormatter.ofPattern("d-M-uuuu").withResolverStyle(java.time.format.ResolverStyle.STRICT),
            DateTimeFormatter.ofPattern("d.M.uuuu").withResolverStyle(java.time.format.ResolverStyle.STRICT),
            DateTimeFormatter.ofPattern("uuuu/M/d").withResolverStyle(java.time.format.ResolverStyle.STRICT),
            DateTimeFormatter.ofPattern("d MMM uuuu", Locale.ENGLISH).withResolverStyle(java.time.format.ResolverStyle.STRICT),
            DateTimeFormatter.ofPattern("d-MMM-uuuu", Locale.ENGLISH).withResolverStyle(java.time.format.ResolverStyle.STRICT),
            DateTimeFormatter.ofPattern("d MMMM uuuu", Locale.ENGLISH).withResolverStyle(java.time.format.ResolverStyle.STRICT));

    /**
     * A date as Kenyan schools write it: day first ("14/03/2012"), or the ISO form
     * ("2012-03-14"). Two-digit years ("14/03/12") are read as the most recent past
     * year that fits. Blank means no date.
     */
    static LocalDate date(String value) {
        String text = value.trim();
        if (text.isEmpty()) {
            return null;
        }
        // Excel sometimes adds a time: "14/03/2012 00:00".
        text = text.replaceFirst("\\s+\\d{1,2}:\\d{2}(:\\d{2})?$", "");
        java.util.regex.Matcher shortYear = java.util.regex.Pattern
                .compile("^(\\d{1,2})[/.-](\\d{1,2})[/.-](\\d{2})$").matcher(text);
        if (shortYear.matches()) {
            int yy = Integer.parseInt(shortYear.group(3));
            int century = 2000 + yy <= LocalDate.now().getYear() ? 2000 : 1900;
            text = shortYear.group(1) + "/" + shortYear.group(2) + "/" + (century + yy);
        }
        for (DateTimeFormatter format : DATE_FORMATS) {
            for (String candidate : new String[]{text, capitaliseMonth(text)}) {
                try {
                    return LocalDate.parse(candidate, format);
                } catch (DateTimeParseException ignored) {
                    // try the next format
                }
            }
        }
        throw new DateTimeParseException("Not a date", value, 0);
    }

    /** "14 mar 2012" becomes "14 Mar 2012" so the month name parses. */
    private static String capitaliseMonth(String text) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("([A-Za-z])([A-Za-z]+)").matcher(text);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            m.appendReplacement(sb, m.group(1).toUpperCase(Locale.ENGLISH) + m.group(2).toLowerCase(Locale.ENGLISH));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private static String relationship(String value) {
        String text = titleCase(value);
        if (text.isEmpty()) {
            return "Guardian";
        }
        return text.length() > 30 ? text.substring(0, 30) : text;
    }

    private static String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
