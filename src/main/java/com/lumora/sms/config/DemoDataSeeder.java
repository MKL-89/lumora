package com.lumora.sms.config;

import com.lumora.sms.entity.*;
import com.lumora.sms.enums.*;
import com.lumora.sms.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Builds a complete, self-consistent Kenyan secondary school so every screen has
 * real data to show. It runs only under the {@code demo} profile and is idempotent:
 * if the school already exists, nothing is written.
 *
 * <p>The generated data is deliberately deterministic. A fixed random seed means
 * the same marks, attendance, and payments appear on every machine, so a
 * demonstration is reproducible.
 */
@Slf4j
@Component
@Profile("demo")
@Order(20)
@RequiredArgsConstructor
public class DemoDataSeeder implements CommandLineRunner {

    private static final String SCHOOL_CODE = "LUM-001";
    private static final String DEFAULT_PASSWORD = "Lumora@123";

    private final SchoolRepository schools;
    private final AcademicYearRepository years;
    private final TermRepository terms;
    private final SchoolClassRepository schoolClasses;
    private final ClassStreamRepository streams;
    private final SubjectRepository subjects;
    private final RoleRepository roles;
    private final UserRepository users;
    private final TeacherRepository teachers;
    private final ParentRepository parents;
    private final StudentRepository students;
    private final StudentGuardianRepository guardians;
    private final StudentEnrollmentRepository enrollments;
    private final TeacherSubjectAssignmentRepository assignments;
    private final GradingScaleRepository gradingScales;
    private final GradeDefinitionRepository gradeDefinitions;
    private final ExaminationRepository examinations;
    private final ExamSubjectRepository examSubjects;
    private final ExamResultRepository examResults;
    private final AttendanceSessionRepository attendanceSessions;
    private final AttendanceRecordRepository attendanceRecords;
    private final FeeCategoryRepository feeCategories;
    private final FeeStructureRepository feeStructures;
    private final FeeStructureItemRepository feeStructureItems;
    private final StudentInvoiceRepository invoices;
    private final PaymentRepository payments;
    private final MpesaTransactionRepository mpesaTransactions;
    private final BookCategoryRepository bookCategories;
    private final BookRepository books;
    private final BookCopyRepository bookCopies;
    private final BookLoanRepository bookLoans;
    private final AnnouncementRepository announcements;
    private final PasswordEncoder encoder;

    private final Random random = new Random(20260722L);

    @Override
    @Transactional
    public void run(String... args) {
        if (schools.findByCode(SCHOOL_CODE).isPresent()) {
            log.info("Demo data already present; skipping seed.");
            return;
        }
        log.info("Seeding the Lumora demo school...");

        School school = createSchool();
        AcademicYear year = createAcademicYear(school);
        List<Term> schoolTerms = createTerms(year);
        Term currentTerm = schoolTerms.get(1);

        List<SchoolClass> classes = createClasses(school);
        List<ClassStream> classStreams = createStreams(classes);
        List<Subject> schoolSubjects = createSubjects(school);
        GradingScale scale = createGradingScale(school);

        createRoles();
        createStaffAccounts();
        List<Teacher> staff = createTeachers();
        assignTeaching(staff, schoolSubjects, classStreams, currentTerm);

        List<Student> learners = createStudents(school, year, classStreams);
        createGuardians(learners);
        linkStudentPortalAccount(learners);

        createExaminations(currentTerm, classStreams, schoolSubjects, scale);
        createAttendance(classStreams, staff);
        createFinance(school, currentTerm, classes, learners);
        createLibrary(school);
        createAnnouncements();

        log.info("Demo seed complete: {} learners, {} teachers, {} streams.",
                students.count(), teachers.count(), streams.count());
    }

    private School createSchool() {
        School school = new School();
        school.setName("Lumora Secondary School");
        school.setCode(SCHOOL_CODE);
        school.setRegistrationNumber("MOE/SEC/2014/0091");
        school.setCounty("Nakuru");
        school.setAddress("P.O. Box 4471-20100, Nakuru");
        school.setPhone("+254 712 004 471");
        school.setEmail("office@lumora.ac.ke");
        return schools.save(school);
    }

    private AcademicYear createAcademicYear(School school) {
        AcademicYear year = new AcademicYear();
        year.setSchool(school);
        year.setName("2026");
        year.setStartDate(LocalDate.of(2026, 1, 5));
        year.setEndDate(LocalDate.of(2026, 11, 27));
        year.setActive(true);
        return years.save(year);
    }

    private List<Term> createTerms(AcademicYear year) {
        Object[][] definitions = {
                {1, "Term 1", LocalDate.of(2026, 1, 5), LocalDate.of(2026, 4, 10), false},
                {2, "Term 2", LocalDate.of(2026, 5, 4), LocalDate.of(2026, 8, 7), true},
                {3, "Term 3", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 11, 27), false}};

        List<Term> created = new ArrayList<>();
        for (Object[] definition : definitions) {
            Term term = new Term();
            term.setAcademicYear(year);
            term.setTermNumber((Integer) definition[0]);
            term.setName((String) definition[1]);
            term.setStartDate((LocalDate) definition[2]);
            term.setEndDate((LocalDate) definition[3]);
            term.setActive((Boolean) definition[4]);
            created.add(terms.save(term));
        }
        return created;
    }

    private List<SchoolClass> createClasses(School school) {
        List<SchoolClass> created = new ArrayList<>();
        String[] names = {"Form 1", "Form 2", "Form 3", "Form 4"};
        for (int i = 0; i < names.length; i++) {
            SchoolClass schoolClass = new SchoolClass();
            schoolClass.setSchool(school);
            schoolClass.setName(names[i]);
            schoolClass.setLevel(i + 1);
            created.add(schoolClasses.save(schoolClass));
        }
        return created;
    }

    private List<ClassStream> createStreams(List<SchoolClass> classes) {
        List<ClassStream> created = new ArrayList<>();
        for (SchoolClass schoolClass : classes) {
            for (String name : new String[]{"North", "South"}) {
                ClassStream stream = new ClassStream();
                stream.setSchoolClass(schoolClass);
                stream.setName(name);
                stream.setCapacity(45);
                created.add(streams.save(stream));
            }
        }
        return created;
    }

    private List<Subject> createSubjects(School school) {
        String[][] definitions = {
                {"MAT", "Mathematics", "true"},
                {"ENG", "English", "true"},
                {"KIS", "Kiswahili", "true"},
                {"BIO", "Biology", "false"},
                {"CHE", "Chemistry", "false"},
                {"PHY", "Physics", "false"},
                {"HIS", "History and Government", "false"},
                {"GEO", "Geography", "false"},
                {"CRE", "Christian Religious Education", "false"},
                {"BST", "Business Studies", "false"}};

        List<Subject> created = new ArrayList<>();
        for (String[] definition : definitions) {
            Subject subject = new Subject();
            subject.setSchool(school);
            subject.setCode(definition[0]);
            subject.setName(definition[1]);
            subject.setCompulsory(Boolean.parseBoolean(definition[2]));
            created.add(subjects.save(subject));
        }
        return created;
    }

    /** The standard KCSE twelve-point scale. */
    private GradingScale createGradingScale(School school) {
        GradingScale scale = new GradingScale();
        scale.setSchool(school);
        scale.setName("KCSE Standard");
        scale.setActive(true);
        scale = gradingScales.save(scale);

        Object[][] bands = {
                {"A", 80, 100, 12, "Excellent"},
                {"A-", 75, 79, 11, "Very good"},
                {"B+", 70, 74, 10, "Very good"},
                {"B", 65, 69, 9, "Good"},
                {"B-", 60, 64, 8, "Good"},
                {"C+", 55, 59, 7, "Above average"},
                {"C", 50, 54, 6, "Average"},
                {"C-", 45, 49, 5, "Average"},
                {"D+", 40, 44, 4, "Below average"},
                {"D", 35, 39, 3, "Weak"},
                {"D-", 30, 34, 2, "Weak"},
                {"E", 0, 29, 1, "Needs urgent support"}};

        for (Object[] band : bands) {
            GradeDefinition definition = new GradeDefinition();
            definition.setGradingScale(scale);
            definition.setGrade((String) band[0]);
            definition.setMinimumMark(BigDecimal.valueOf((Integer) band[1]));
            definition.setMaximumMark(BigDecimal.valueOf((Integer) band[2]));
            definition.setPoints((Integer) band[3]);
            definition.setRemarks((String) band[4]);
            gradeDefinitions.save(definition);
        }
        return scale;
    }

    private void createRoles() {
        Map<UserRoleType, String> descriptions = Map.ofEntries(
                Map.entry(UserRoleType.SUPER_ADMIN, "Platform governance, school setup and global configuration."),
                Map.entry(UserRoleType.ADMINISTRATOR, "Day-to-day school administration and operational reports."),
                Map.entry(UserRoleType.PRINCIPAL, "Whole-school academics, finance summaries and approvals."),
                Map.entry(UserRoleType.DEPUTY_PRINCIPAL, "Academic operations, examinations and attendance oversight."),
                Map.entry(UserRoleType.REGISTRAR, "Admissions, student records and class allocation."),
                Map.entry(UserRoleType.BURSAR, "Fee structures, invoices, receipts and arrears."),
                Map.entry(UserRoleType.TEACHER, "Assigned subjects: attendance, marks and progress."),
                Map.entry(UserRoleType.CLASS_TEACHER, "Teacher duties plus a stream roster and guardian contact."),
                Map.entry(UserRoleType.STUDENT, "Own timetable, attendance, results, fees and loans."),
                Map.entry(UserRoleType.PARENT, "Linked children only: attendance, results and fee balances."),
                Map.entry(UserRoleType.LIBRARIAN, "Catalogue, copies, circulation and overdue items."));

        for (UserRoleType type : UserRoleType.values()) {
            if (roles.findByName(type).isEmpty()) {
                Role role = new Role();
                role.setName(type);
                role.setDescription(descriptions.getOrDefault(type, type.name()));
                roles.save(role);
            }
        }
    }

    private User account(String username, String email, UserRoleType... granted) {
        return users.findByUsername(username).orElseGet(() -> {
            User user = new User();
            user.setUsername(username);
            user.setEmail(email);
            user.setPasswordHash(encoder.encode(DEFAULT_PASSWORD));
            user.setPasswordChangedAt(LocalDateTime.now());
            Set<Role> assigned = new HashSet<>();
            for (UserRoleType type : granted) {
                roles.findByName(type).ifPresent(assigned::add);
            }
            user.setRoles(assigned);
            return users.save(user);
        });
    }

    /** One sign-in account per administrative role. */
    private void createStaffAccounts() {
        account("superadmin", "superadmin@lumora.ac.ke", UserRoleType.SUPER_ADMIN);
        account("admin", "admin@lumora.ac.ke", UserRoleType.ADMINISTRATOR);
        account("principal", "principal@lumora.ac.ke", UserRoleType.PRINCIPAL);
        account("deputy", "deputy@lumora.ac.ke", UserRoleType.DEPUTY_PRINCIPAL);
        account("registrar", "registrar@lumora.ac.ke", UserRoleType.REGISTRAR);
        account("bursar", "bursar@lumora.ac.ke", UserRoleType.BURSAR);
        account("librarian", "librarian@lumora.ac.ke", UserRoleType.LIBRARIAN);
    }

    private List<Teacher> createTeachers() {
        String[][] definitions = {
                {"T/001", "Grace", "Wanjiru", "FEMALE", "+254720110001", "TSC/558201", "CLASS_TEACHER"},
                {"T/002", "Peter", "Otieno", "MALE", "+254720110002", "TSC/558202", "CLASS_TEACHER"},
                {"T/003", "Mercy", "Chebet", "FEMALE", "+254720110003", "TSC/558203", "TEACHER"},
                {"T/004", "Samuel", "Kariuki", "MALE", "+254720110004", "TSC/558204", "TEACHER"},
                {"T/005", "Alice", "Nafula", "FEMALE", "+254720110005", "TSC/558205", "TEACHER"},
                {"T/006", "Daniel", "Mutiso", "MALE", "+254720110006", "TSC/558206", "TEACHER"}};

        List<Teacher> created = new ArrayList<>();
        for (String[] definition : definitions) {
            String username = (definition[1] + "." + definition[2]).toLowerCase();
            UserRoleType roleType = UserRoleType.valueOf(definition[6]);
            User user = roleType == UserRoleType.CLASS_TEACHER
                    ? account(username, username + "@lumora.ac.ke", UserRoleType.TEACHER, UserRoleType.CLASS_TEACHER)
                    : account(username, username + "@lumora.ac.ke", UserRoleType.TEACHER);

            Teacher teacher = new Teacher();
            teacher.setUser(user);
            teacher.setEmployeeNumber(definition[0]);
            teacher.setFirstName(definition[1]);
            teacher.setLastName(definition[2]);
            teacher.setGender(Gender.valueOf(definition[3]));
            teacher.setPhone(definition[4]);
            teacher.setTscNumber(definition[5]);
            teacher.setEmploymentDate(LocalDate.of(2015 + created.size(), 1, 15));
            created.add(teachers.save(teacher));
        }
        return created;
    }

    /** Spreads the ten subjects across the six teachers for every stream. */
    private void assignTeaching(List<Teacher> staff, List<Subject> schoolSubjects,
                                List<ClassStream> classStreams, Term term) {
        for (ClassStream stream : classStreams) {
            for (int i = 0; i < schoolSubjects.size(); i++) {
                TeacherSubjectAssignment assignment = new TeacherSubjectAssignment();
                assignment.setTeacher(staff.get(i % staff.size()));
                assignment.setSubject(schoolSubjects.get(i));
                assignment.setClassStream(stream);
                assignment.setTerm(term);
                assignments.save(assignment);
            }
        }
    }

    private List<Student> createStudents(School school, AcademicYear year, List<ClassStream> classStreams) {
        String[] firstNames = {"Brian", "Faith", "Kevin", "Mercy", "John", "Cynthia", "Dennis", "Joy",
                               "Victor", "Esther", "Collins", "Sharon", "Ian", "Purity", "Elvis", "Naomi",
                               "Felix", "Beatrice", "Erick", "Lydia", "Kelvin", "Winnie", "Moses", "Caroline"};
        String[] lastNames = {"Kamau", "Achieng", "Kiprop", "Wafula", "Njoroge", "Atieno", "Barasa", "Mwende",
                              "Omondi", "Wangui", "Kipchoge", "Auma", "Muriithi", "Chelimo", "Ochieng", "Nyambura"};
        String[] counties = {"Nakuru", "Kiambu", "Kisumu", "Uasin Gishu", "Bungoma", "Machakos"};

        List<Student> created = new ArrayList<>();
        int admissionCounter = 1;

        for (ClassStream stream : classStreams) {
            int level = stream.getSchoolClass().getLevel();
            for (int i = 0; i < 12; i++) {
                Student student = new Student();
                student.setSchool(school);
                student.setAdmissionNumber(String.format("ADM/2026/%03d", admissionCounter++));
                student.setFirstName(firstNames[random.nextInt(firstNames.length)]);
                student.setLastName(lastNames[random.nextInt(lastNames.length)]);
                student.setGender(random.nextBoolean() ? Gender.MALE : Gender.FEMALE);
                student.setDateOfBirth(LocalDate.of(2026 - 14 - level, 1 + random.nextInt(12), 1 + random.nextInt(28)));
                student.setAdmissionDate(LocalDate.of(2027 - level, 1, 8));
                student.setCounty(counties[random.nextInt(counties.length)]);
                student.setActive(true);
                student = students.save(student);

                StudentEnrollment enrollment = new StudentEnrollment();
                enrollment.setStudent(student);
                enrollment.setAcademicYear(year);
                enrollment.setClassStream(stream);
                enrollment.setActive(true);
                enrollments.save(enrollment);

                created.add(student);
            }
        }
        return created;
    }

    /** Gives every fourth learner a guardian, and the first guardian a portal login. */
    private void createGuardians(List<Student> learners) {
        String[] occupations = {"Farmer", "Teacher", "Trader", "Nurse", "Driver", "Engineer"};
        int counter = 0;

        for (int i = 0; i < learners.size(); i += 4) {
            Student child = learners.get(i);
            counter++;

            User user = counter == 1 ? account("parent", "parent@lumora.ac.ke", UserRoleType.PARENT) : null;

            Parent parent = new Parent();
            parent.setUser(user);
            parent.setFirstName(counter == 1 ? "Joseph" : "Guardian" + counter);
            parent.setLastName(child.getLastName());
            parent.setPhone("+2547" + String.format("%08d", 20000000 + counter));
            parent.setEmail(counter == 1 ? "parent@lumora.ac.ke" : "guardian" + counter + "@example.co.ke");
            parent.setOccupation(occupations[counter % occupations.length]);
            parent = parents.save(parent);

            StudentGuardian link = new StudentGuardian();
            link.setStudent(child);
            link.setParent(parent);
            link.setRelationship(counter % 2 == 0 ? "Mother" : "Father");
            link.setPrimaryContact(true);
            guardians.save(link);

            // The demo guardian also covers a second child, so the parent portal shows more than one.
            if (counter == 1 && learners.size() > 1) {
                StudentGuardian second = new StudentGuardian();
                second.setStudent(learners.get(1));
                second.setParent(parent);
                second.setRelationship("Father");
                second.setPrimaryContact(false);
                guardians.save(second);
            }
        }
    }

    /** Gives the first learner a portal login so the Student dashboard can be shown. */
    private void linkStudentPortalAccount(List<Student> learners) {
        if (learners.isEmpty()) {
            return;
        }
        Student first = learners.get(0);
        first.setUser(account("student", "student@lumora.ac.ke", UserRoleType.STUDENT));
        students.save(first);
    }

    /**
     * Two examinations per stream. The opener is published and fully marked; the
     * mid-term is left unmarked so mark entry and publication can be demonstrated
     * live.
     */
    private void createExaminations(Term term, List<ClassStream> classStreams,
                                    List<Subject> schoolSubjects, GradingScale scale) {
        List<GradeDefinition> bands = gradeDefinitions.findByGradingScaleIdOrderByMinimumMarkDesc(scale.getId());

        for (ClassStream stream : classStreams) {
            String suffix = " - " + stream.getSchoolClass().getName() + " " + stream.getName();

            for (String[] definition : new String[][]{{"Opener Examination", "2026-05-20", "true"},
                                                      {"Mid-Term Examination", "2026-06-24", "false"}}) {
                Examination examination = new Examination();
                examination.setTerm(term);
                examination.setClassStream(stream);
                examination.setGradingScale(scale);
                examination.setName(definition[0] + suffix);
                examination.setExaminationDate(LocalDate.parse(definition[1]));
                examination.setPublished(Boolean.parseBoolean(definition[2]));
                examination = examinations.save(examination);

                List<Student> candidates = students.findEnrolledInStream(stream.getId());

                // Only the first four subjects are examined, keeping the seed quick.
                for (Subject subject : schoolSubjects.subList(0, 4)) {
                    ExamSubject examSubject = new ExamSubject();
                    examSubject.setExamination(examination);
                    examSubject.setSubject(subject);
                    examSubject.setMaximumMark(BigDecimal.valueOf(100));
                    examSubject = examSubjects.save(examSubject);

                    if (!examination.isPublished()) {
                        continue;
                    }
                    for (Student candidate : candidates) {
                        BigDecimal mark = BigDecimal.valueOf(28 + random.nextInt(65));
                        ExamResult result = new ExamResult();
                        result.setExamSubject(examSubject);
                        result.setStudent(candidate);
                        result.setMarks(mark);
                        applyBand(result, mark, bands);
                        examResults.save(result);
                    }
                }
            }
        }
    }

    private void applyBand(ExamResult result, BigDecimal percentage, List<GradeDefinition> bands) {
        for (GradeDefinition band : bands) {
            if (percentage.compareTo(band.getMinimumMark()) >= 0
                    && percentage.compareTo(band.getMaximumMark()) <= 0) {
                result.setGrade(band.getGrade());
                result.setPoints(band.getPoints());
                result.setRemarks(band.getRemarks());
                return;
            }
        }
    }

    /** Ten school days of registers, so the attendance trend chart has a shape. */
    private void createAttendance(List<ClassStream> classStreams, List<Teacher> staff) {
        LocalDate day = LocalDate.now();
        int created = 0;

        while (created < 10) {
            if (day.getDayOfWeek().getValue() >= 6) {
                day = day.minusDays(1);
                continue;
            }
            for (ClassStream stream : classStreams) {
                AttendanceSession session = new AttendanceSession();
                session.setClassStream(stream);
                session.setRecordedBy(staff.get(random.nextInt(staff.size())));
                session.setAttendanceDate(day);
                session = attendanceSessions.save(session);

                for (Student student : students.findEnrolledInStream(stream.getId())) {
                    int roll = random.nextInt(100);
                    AttendanceStatus status = roll < 88 ? AttendanceStatus.PRESENT
                            : roll < 94 ? AttendanceStatus.LATE
                            : roll < 98 ? AttendanceStatus.ABSENT
                            : AttendanceStatus.EXCUSED;

                    AttendanceRecord record = new AttendanceRecord();
                    record.setAttendanceSession(session);
                    record.setStudent(student);
                    record.setStatus(status);
                    if (status == AttendanceStatus.EXCUSED) {
                        record.setRemarks("Medical appointment");
                    }
                    attendanceRecords.save(record);
                }
            }
            created++;
            day = day.minusDays(1);
        }
    }

    private void createFinance(School school, Term term, List<SchoolClass> classes, List<Student> learners) {
        Map<String, FeeCategory> categories = new LinkedHashMap<>();
        String[][] categoryDefinitions = {{"Tuition", "Termly tuition charge"},
                                          {"Boarding", "Accommodation and meals"},
                                          {"Activity", "Games, clubs and trips"},
                                          {"Development", "Infrastructure levy"}};
        for (String[] definition : categoryDefinitions) {
            FeeCategory category = new FeeCategory();
            category.setSchool(school);
            category.setName(definition[0]);
            category.setDescription(definition[1]);
            category.setActive(true);
            categories.put(definition[0], feeCategories.save(category));
        }

        // Senior classes carry a higher tuition charge.
        for (SchoolClass schoolClass : classes) {
            FeeStructure structure = new FeeStructure();
            structure.setTerm(term);
            structure.setSchoolClass(schoolClass);
            structure.setCurrencyCode("KES");
            structure.setActive(true);
            structure = feeStructures.save(structure);

            addItem(structure, categories.get("Tuition"), 18_000 + (schoolClass.getLevel() * 1_500));
            addItem(structure, categories.get("Boarding"), 12_000);
            addItem(structure, categories.get("Activity"), 2_500);
            addItem(structure, categories.get("Development"), 3_000);
        }

        // Invoice every learner, then part-pay a realistic share of them.
        int receipt = 1;
        for (Student learner : learners) {
            StudentEnrollment enrollment = enrollments.findByStudentIdAndActiveTrue(learner.getId()).orElse(null);
            if (enrollment == null) {
                continue;
            }
            FeeStructure structure = feeStructures.findByTermIdAndSchoolClassId(
                    term.getId(), enrollment.getClassStream().getSchoolClass().getId()).orElse(null);
            if (structure == null) {
                continue;
            }
            BigDecimal total = feeStructureItems.totalFor(structure.getId());

            StudentInvoice invoice = new StudentInvoice();
            invoice.setStudent(learner);
            invoice.setTerm(term);
            invoice.setInvoiceNumber("INV-2026T" + term.getTermNumber() + "-" + learner.getAdmissionNumber());
            invoice.setTotalAmount(total);
            invoice.setBalanceAmount(total);
            invoice.setStatus(InvoiceStatus.ISSUED);
            invoice.setDueDate(term.getStartDate().plusDays(30));
            invoice = invoices.save(invoice);

            int roll = random.nextInt(100);
            if (roll < 25) {
                continue; // Nothing paid yet.
            }
            BigDecimal paid = roll < 70
                    ? total.multiply(BigDecimal.valueOf(40 + random.nextInt(45)))
                           .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                    : total;

            PaymentMethod method = roll % 3 == 0 ? PaymentMethod.MPESA
                    : roll % 3 == 1 ? PaymentMethod.BANK_TRANSFER : PaymentMethod.CASH;

            Payment payment = new Payment();
            payment.setStudentInvoice(invoice);
            payment.setReceiptNumber(String.format("RCP-2026-%04d", receipt));
            payment.setAmount(paid);
            payment.setMethod(method);
            payment.setPaidAt(LocalDateTime.now().minusDays(random.nextInt(45)));
            payment.setReferenceNumber(method == PaymentMethod.MPESA
                    ? null : "TRF" + (100000 + random.nextInt(899999)));
            payment = payments.save(payment);

            invoice.setBalanceAmount(total.subtract(paid));
            invoice.setStatus(invoice.getBalanceAmount().signum() == 0
                    ? InvoiceStatus.PAID : InvoiceStatus.PARTIALLY_PAID);
            invoices.save(invoice);

            if (method == PaymentMethod.MPESA) {
                MpesaTransaction transaction = new MpesaTransaction();
                transaction.setPayment(payment);
                transaction.setTransactionCode("SGL" + String.format("%06d", 100000 + receipt));
                transaction.setPayerPhone("+2547" + String.format("%08d", 30000000 + receipt));
                transaction.setAmount(paid);
                transaction.setTransactionTime(payment.getPaidAt());
                transaction.setAccountReference(learner.getAdmissionNumber());
                mpesaTransactions.save(transaction);
            }
            receipt++;
        }
    }

    private void addItem(FeeStructure structure, FeeCategory category, int amount) {
        FeeStructureItem item = new FeeStructureItem();
        item.setFeeStructure(structure);
        item.setFeeCategory(category);
        item.setAmount(BigDecimal.valueOf(amount));
        feeStructureItems.save(item);
    }

    private void createLibrary(School school) {
        Map<String, BookCategory> categories = new LinkedHashMap<>();
        for (String name : new String[]{"Set Books", "Sciences", "Mathematics", "Reference"}) {
            BookCategory category = new BookCategory();
            category.setSchool(school);
            category.setName(name);
            category.setDescription(name + " collection");
            categories.put(name, bookCategories.save(category));
        }

        String[][] titles = {
                {"The River and the Source", "Margaret A. Ogola", "Set Books", "9789966465689"},
                {"Blossoms of the Savannah", "Henry ole Kulet", "Set Books", "9789966250384"},
                {"A Doll's House", "Henrik Ibsen", "Set Books", "9780486270623"},
                {"KLB Secondary Biology Form 3", "Kenya Literature Bureau", "Sciences", "9789966102201"},
                {"KLB Secondary Chemistry Form 4", "Kenya Literature Bureau", "Sciences", "9789966102218"},
                {"Longhorn Secondary Physics", "Longhorn Publishers", "Sciences", "9789966499103"},
                {"KLB Secondary Mathematics Form 2", "Kenya Literature Bureau", "Mathematics", "9789966102225"},
                {"Oxford English Dictionary", "Oxford University Press", "Reference", "9780199571123"}};

        List<BookCopy> allCopies = new ArrayList<>();
        int accession = 1;
        for (String[] title : titles) {
            Book book = new Book();
            book.setCategory(categories.get(title[2]));
            book.setTitle(title[0]);
            book.setAuthor(title[1]);
            book.setIsbn(title[3]);
            book.setPublisher(title[1]);
            book.setPublicationYear(2015 + random.nextInt(9));
            book = books.save(book);

            for (int i = 0; i < 4; i++) {
                BookCopy copy = new BookCopy();
                copy.setBook(book);
                copy.setAccessionNumber(String.format("LUM/LIB/%04d", accession++));
                copy.setStatus("AVAILABLE");
                allCopies.add(bookCopies.save(copy));
            }
        }

        // A handful of live loans, the first of which is deliberately overdue.
        List<User> borrowers = new ArrayList<>(users.findByRolesName(UserRoleType.STUDENT));
        borrowers.addAll(users.findByRolesName(UserRoleType.TEACHER));
        if (borrowers.isEmpty()) {
            return;
        }
        for (int i = 0; i < 6 && i < allCopies.size(); i++) {
            BookCopy copy = allCopies.get(i);
            LocalDate borrowedOn = LocalDate.now().minusDays(i == 0 ? 30 : random.nextInt(10));

            BookLoan loan = new BookLoan();
            loan.setBookCopy(copy);
            loan.setBorrower(borrowers.get(i % borrowers.size()));
            loan.setBorrowedOn(borrowedOn);
            loan.setDueDate(borrowedOn.plusDays(14));
            loan.setStatus(LoanStatus.BORROWED);
            bookLoans.save(loan);

            copy.setStatus("ISSUED");
            bookCopies.save(copy);
        }
    }

    private void createAnnouncements() {
        User author = users.findByUsername("principal").orElse(null);
        if (author == null) {
            return;
        }
        String[][] notices = {
                {"Term 2 opening",
                 "Term 2 begins on Monday 4 May 2026. All learners must report by 8:00 a.m. in full school uniform.",
                 "60"},
                {"Mid-term examinations",
                 "Mid-term examinations run from 24 to 28 June 2026. Revision timetables are with class teachers.",
                 "30"},
                {"Fee balances",
                 "Guardians with outstanding balances should clear them at the bursar office or via M-Pesa "
                 + "before the mid-term break.",
                 "45"},
                {"Library stock-take",
                 "The library closes for stock-take on the last Friday of the month. Please return all borrowed copies.",
                 "14"}};

        for (String[] notice : notices) {
            Announcement announcement = new Announcement();
            announcement.setCreatedBy(author);
            announcement.setTitle(notice[0]);
            announcement.setContent(notice[1]);
            announcement.setPublishedAt(LocalDateTime.now().minusDays(random.nextInt(10)));
            announcement.setExpiresAt(LocalDateTime.now().plusDays(Integer.parseInt(notice[2])));
            announcements.save(announcement);
        }
    }
}
