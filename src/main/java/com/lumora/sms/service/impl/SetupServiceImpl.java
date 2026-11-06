package com.lumora.sms.service.impl;

import com.lumora.sms.config.SchoolDefaults;
import com.lumora.sms.dto.SetupRequest;
import com.lumora.sms.entity.*;
import com.lumora.sms.enums.UserRoleType;
import com.lumora.sms.exception.BusinessRuleViolationException;
import com.lumora.sms.repository.*;
import com.lumora.sms.security.PasswordRules;
import com.lumora.sms.service.SetupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/** Builds a school from nothing in one transaction: either all of it is created or none. */
@Slf4j
@Service
@RequiredArgsConstructor
public class SetupServiceImpl implements SetupService {

    private final UserRepository users;
    private final RoleRepository roles;
    private final SchoolRepository schools;
    private final AcademicYearRepository years;
    private final TermRepository terms;
    private final SchoolClassRepository schoolClasses;
    private final ClassStreamRepository streams;
    private final SubjectRepository subjects;
    private final GradingScaleRepository gradingScales;
    private final GradeDefinitionRepository gradeDefinitions;
    private final PasswordEncoder encoder;

    @Override
    @Transactional(readOnly = true)
    public boolean needed() {
        return users.count() == 0;
    }

    @Override
    @Transactional
    public User run(SetupRequest r) {
        if (users.count() > 0 || schools.count() > 0) {
            throw new BusinessRuleViolationException("This school is already set up. Please sign in.");
        }
        if (!r.adminPassword().equals(r.adminPasswordConfirm())) {
            throw new BusinessRuleViolationException("The two passwords do not match.");
        }
        PasswordRules.check(r.adminPassword(), r.adminUsername());

        List<String> streamNames = streamNames(r.streams());
        Set<String> chosenSubjects = new HashSet<>(r.subjects() == null ? List.of() : r.subjects());
        if (SchoolDefaults.KCSE_SUBJECTS.stream().noneMatch(s -> chosenSubjects.contains(s.code()))) {
            throw new BusinessRuleViolationException("Tick at least one subject the school offers.");
        }

        createRoles();
        School school = createSchool(r);
        AcademicYear year = createYear(school, r.year());
        createTerms(year, r.year(), r.currentTerm());
        createFormsAndStreams(school, streamNames);
        createSubjects(school, chosenSubjects);
        createGradingScale(school);

        User admin = new User();
        admin.setUsername(r.adminUsername().trim());
        admin.setEmail(r.adminEmail().trim());
        admin.setPasswordHash(encoder.encode(r.adminPassword()));
        admin.setPasswordChangedAt(LocalDateTime.now());
        admin.setRoles(new HashSet<>(Set.of(role(UserRoleType.SUPER_ADMIN), role(UserRoleType.ADMINISTRATOR))));
        admin = users.save(admin);

        log.info("First-time setup complete for {} with {} streams per form.", school.getName(), streamNames.size());
        return admin;
    }

    /** "East, West" becomes [East, West]. At least one stream, at most eight, no repeats. */
    static List<String> streamNames(String raw) {
        LinkedHashMap<String, String> names = new LinkedHashMap<>();
        for (String part : (raw == null ? "" : raw).split("[,;\\r\\n]+")) {
            String name = part.trim().replaceAll("\\s+", " ");
            if (!name.isEmpty()) {
                if (name.length() > 40) {
                    throw new BusinessRuleViolationException("A stream name can be at most 40 characters: " + name);
                }
                names.putIfAbsent(name.toLowerCase(Locale.ROOT), name);
            }
        }
        if (names.isEmpty()) {
            throw new BusinessRuleViolationException("Give at least one stream name, for example: East, West.");
        }
        if (names.size() > 8) {
            throw new BusinessRuleViolationException("A form can have at most eight streams.");
        }
        return List.copyOf(names.values());
    }

    private void createRoles() {
        for (UserRoleType type : UserRoleType.values()) {
            if (roles.findByName(type).isEmpty()) {
                Role role = new Role();
                role.setName(type);
                role.setDescription(SchoolDefaults.ROLE_DESCRIPTIONS.getOrDefault(type, type.name()));
                roles.save(role);
            }
        }
    }

    private Role role(UserRoleType type) {
        return roles.findByName(type).orElseThrow();
    }

    private School createSchool(SetupRequest r) {
        School school = new School();
        school.setName(r.schoolName().trim());
        school.setCode(r.schoolCode() == null || r.schoolCode().isBlank() ? "SCH-001" : r.schoolCode().trim());
        school.setCounty(blankToNull(r.county()));
        school.setPhone(blankToNull(r.phone()));
        school.setEmail(blankToNull(r.schoolEmail()));
        return schools.save(school);
    }

    private AcademicYear createYear(School school, int calendarYear) {
        AcademicYear year = new AcademicYear();
        year.setSchool(school);
        year.setName(String.valueOf(calendarYear));
        year.setStartDate(LocalDate.of(calendarYear, 1, 1));
        year.setEndDate(LocalDate.of(calendarYear, 12, 31));
        year.setActive(true);
        return years.save(year);
    }

    /** The usual Kenyan three-term calendar. */
    private void createTerms(AcademicYear year, int y, int current) {
        LocalDate[][] dates = {
                {LocalDate.of(y, 1, 6), LocalDate.of(y, 4, 4)},
                {LocalDate.of(y, 4, 28), LocalDate.of(y, 8, 1)},
                {LocalDate.of(y, 8, 25), LocalDate.of(y, 11, 21)}};
        for (int i = 0; i < dates.length; i++) {
            Term term = new Term();
            term.setAcademicYear(year);
            term.setTermNumber(i + 1);
            term.setName("Term " + (i + 1));
            term.setStartDate(dates[i][0]);
            term.setEndDate(dates[i][1]);
            term.setActive(i + 1 == current);
            terms.save(term);
        }
    }

    private void createFormsAndStreams(School school, List<String> streamNames) {
        for (int level = 1; level <= 4; level++) {
            SchoolClass form = new SchoolClass();
            form.setSchool(school);
            form.setName("Form " + level);
            form.setLevel(level);
            form = schoolClasses.save(form);
            for (String name : streamNames) {
                ClassStream stream = new ClassStream();
                stream.setSchoolClass(form);
                stream.setName(name);
                stream.setCapacity(45);
                streams.save(stream);
            }
        }
    }

    private void createSubjects(School school, Set<String> chosen) {
        for (SchoolDefaults.SubjectOption option : SchoolDefaults.KCSE_SUBJECTS) {
            if (chosen.contains(option.code())) {
                Subject subject = new Subject();
                subject.setSchool(school);
                subject.setCode(option.code());
                subject.setName(option.name());
                subject.setCompulsory(option.compulsory());
                subjects.save(subject);
            }
        }
    }

    private void createGradingScale(School school) {
        GradingScale scale = new GradingScale();
        scale.setSchool(school);
        scale.setName("KCSE Standard");
        scale.setActive(true);
        scale = gradingScales.save(scale);
        for (SchoolDefaults.GradeBand band : SchoolDefaults.KCSE_GRADES) {
            GradeDefinition definition = new GradeDefinition();
            definition.setGradingScale(scale);
            definition.setGrade(band.grade());
            definition.setMinimumMark(BigDecimal.valueOf(band.min()));
            definition.setMaximumMark(BigDecimal.valueOf(band.max()));
            definition.setPoints(band.points());
            definition.setRemarks(band.remarks());
            gradeDefinitions.save(definition);
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
