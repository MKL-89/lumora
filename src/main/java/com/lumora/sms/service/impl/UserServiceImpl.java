package com.lumora.sms.service.impl;

import com.lumora.sms.dto.*;
import com.lumora.sms.entity.*;
import com.lumora.sms.enums.UserRoleType;
import com.lumora.sms.exception.*;
import com.lumora.sms.repository.*;
import com.lumora.sms.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Default transactional implementation for user lifecycle operations. */
@Service
@org.springframework.validation.annotation.Validated
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository users;
    private final RoleRepository roles;
    private final ParentRepository parents;
    private final PasswordEncoder passwordEncoder;
    private final TeacherRepository teachers;
    private final StudentRepository students;
    private final StudentGuardianRepository guardians;

    @Override
    public String displayName(String username) {
        if (username == null || username.isBlank()) {
            return "";
        }
        return teachers.findByUserUsername(username).map(t -> t.getFirstName() + " " + t.getLastName())
                .or(() -> students.findByUserUsername(username).map(s -> s.getFirstName() + " " + s.getLastName()))
                .or(() -> parents.findByUserUsername(username).map(p -> p.getFirstName() + " " + p.getLastName()))
                .orElseGet(() -> tidy(username));
    }

    /** "grace.wanjiru" becomes "Grace Wanjiru"; "superadmin" becomes "Superadmin". */
    static String tidy(String username) {
        return java.util.Arrays.stream(username.split("[._\\-]+"))
                .filter(part -> !part.isEmpty())
                .map(part -> Character.toUpperCase(part.charAt(0)) + part.substring(1))
                .collect(Collectors.joining(" "));
    }

    /** Letters and digits that cannot be mistaken for each other when read aloud or written down. */
    private static final String TEMP_LETTERS = "abcdefghjkmnpqrstuvwxyz";
    private static final String TEMP_DIGITS = "23456789";
    private static final java.security.SecureRandom RANDOM = new java.security.SecureRandom();

    private static final Set<UserRoleType> TEACHING = EnumSet.of(UserRoleType.TEACHER, UserRoleType.CLASS_TEACHER);

    /** A temporary password such as "kmqt-4827-hzra": easy to read out, used once. */
    static String temporaryPassword() {
        StringBuilder sb = new StringBuilder();
        for (int group = 0; group < 3; group++) {
            if (group > 0) {
                sb.append('-');
            }
            String alphabet = group == 1 ? TEMP_DIGITS : TEMP_LETTERS;
            for (int i = 0; i < 4; i++) {
                sb.append(alphabet.charAt(RANDOM.nextInt(alphabet.length())));
            }
        }
        return sb.toString();
    }

    @Override
    @Transactional
    public String createAccount(@Valid AccountCreateRequest r, boolean actorIsSuperAdmin) {
        String username = r.username().trim();
        String email = r.email().trim();
        if (r.roles().contains(UserRoleType.SUPER_ADMIN) && !actorIsSuperAdmin) {
            throw new BusinessRuleViolationException("Only a super administrator can create another super administrator.");
        }
        if (users.existsByUsername(username)) {
            throw new DuplicateResourceException("Username", username);
        }
        if (users.existsByEmail(email)) {
            throw new DuplicateResourceException("Email", email);
        }
        boolean teaching = r.roles().stream().anyMatch(TEACHING::contains);
        boolean learner = r.roles().contains(UserRoleType.STUDENT);
        boolean guardian = r.roles().contains(UserRoleType.PARENT);
        if (learner && guardian) {
            throw new BusinessRuleViolationException("One account cannot be both a learner and a guardian.");
        }
        if ((learner || guardian) && r.roles().size() > 1) {
            throw new BusinessRuleViolationException("Learner and guardian accounts cannot hold staff roles as well.");
        }
        if (teaching && isBlank(r.employeeNumber())) {
            throw new BusinessRuleViolationException("Teachers need an employee number.");
        }
        if (teaching && teachers.findByEmployeeNumber(r.employeeNumber().trim()).isPresent()) {
            throw new DuplicateResourceException("Employee number", r.employeeNumber().trim());
        }
        Student child = null;
        if (learner || guardian) {
            if (isBlank(r.admissionNumber())) {
                throw new BusinessRuleViolationException("Give the learner's admission number to link this account to.");
            }
            child = students.findByAdmissionNumber(r.admissionNumber().trim())
                    .or(() -> students.findByAdmissionNumber(r.admissionNumber().trim().toUpperCase()))
                    .orElseThrow(() -> new BusinessRuleViolationException(
                            "No learner has admission number " + r.admissionNumber().trim() + "."));
            if (learner && child.getUser() != null) {
                throw new BusinessRuleViolationException(child.getAdmissionNumber() + " already has a portal account ("
                        + child.getUser().getUsername() + ").");
            }
        }
        if (guardian && isBlank(r.phone())) {
            throw new BusinessRuleViolationException("Guardians need a phone number.");
        }

        String temporary = temporaryPassword();
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(temporary));
        user.setMustChangePassword(true);
        user.setRoles(r.roles().stream()
                .map(role -> roles.findByName(role).orElseThrow(() -> new ResourceNotFoundException("Role", role)))
                .collect(Collectors.toCollection(java.util.HashSet::new)));
        user = users.save(user);

        if (teaching) {
            Teacher teacher = new Teacher();
            teacher.setUser(user);
            teacher.setEmployeeNumber(r.employeeNumber().trim());
            teacher.setFirstName(r.firstName().trim());
            teacher.setLastName(r.lastName().trim());
            teacher.setPhone(blankToNull(r.phone()));
            teacher.setTscNumber(blankToNull(r.tscNumber()));
            teacher.setEmploymentDate(java.time.LocalDate.now());
            teachers.save(teacher);
        }
        if (learner) {
            child.setUser(user);
        }
        if (guardian) {
            linkGuardian(user, r, child);
        }
        return temporary;
    }

    /**
     * Links a guardian account to the learner. A guardian already on the learner's
     * record with the same phone number (imported from the old system, say) is
     * given the login; otherwise a new guardian record is added to the learner.
     */
    private void linkGuardian(User user, AccountCreateRequest r, Student child) {
        String phone = r.phone().trim();
        Parent parent = guardians.findByStudentId(child.getId()).stream()
                .map(StudentGuardian::getParent)
                .filter(p -> p.getUser() == null && Phones.same(p.getPhone(), phone))
                .findFirst()
                .orElse(null);
        if (parent == null) {
            parent = new Parent();
            parent.setFirstName(r.firstName().trim());
            parent.setLastName(r.lastName().trim());
            parent.setPhone(phone);
            parent = parents.save(parent);
            StudentGuardian link = new StudentGuardian();
            link.setStudent(child);
            link.setParent(parent);
            link.setRelationship("Guardian");
            link.setPrimaryContact(guardians.findByStudentId(child.getId()).isEmpty());
            guardians.save(link);
        }
        parent.setUser(user);
        parent.setEmail(user.getEmail());
    }

    @Override
    @Transactional
    public String resetPassword(Long id, boolean actorIsSuperAdmin) {
        User user = get(id);
        boolean targetIsSuperAdmin = user.getRoles().stream().anyMatch(r -> r.getName() == UserRoleType.SUPER_ADMIN);
        if (targetIsSuperAdmin && !actorIsSuperAdmin) {
            throw new BusinessRuleViolationException("Only a super administrator can reset a super administrator's password.");
        }
        String temporary = temporaryPassword();
        user.setPasswordHash(passwordEncoder.encode(temporary));
        user.setMustChangePassword(true);
        user.setLocked(false);
        user.setFailedLoginAttempts(0);
        user.setLockoutUntil(null);
        return temporary;
    }

    @Override
    @Transactional
    public void changeOwnPassword(String username, String currentPassword, String newPassword, String confirmation) {
        User user = users.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", username));
        if (currentPassword == null || !passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new BusinessRuleViolationException("Your current password is not correct.");
        }
        if (newPassword == null || !newPassword.equals(confirmation)) {
            throw new BusinessRuleViolationException("The two new passwords do not match.");
        }
        if (newPassword.equals(currentPassword)) {
            throw new BusinessRuleViolationException("Choose a password different from the current one.");
        }
        com.lumora.sms.security.PasswordRules.check(newPassword, username);
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setPasswordChangedAt(java.time.LocalDateTime.now());
        user.setMustChangePassword(false);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String blankToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    @Override
    public User get(Long id) {
        return users.findById(id).orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    @Override
    @Transactional
    public User create(@Valid UserCreateRequest r) {
        if (users.existsByUsername(r.username())) {
            throw new DuplicateResourceException("Username", r.username());
        }
        if (users.existsByEmail(r.email())) {
            throw new DuplicateResourceException("Email", r.email());
        }
        User user = new User();
        user.setUsername(r.username());
        user.setEmail(r.email());
        user.setPasswordHash(passwordEncoder.encode(r.rawPassword()));
        user.setPasswordChangedAt(java.time.LocalDateTime.now());
        user.setRoles(r.roles().stream()
                .map(role -> roles.findByName(role)
                        .orElseThrow(() -> new ResourceNotFoundException("Role", role)))
                .collect(Collectors.toSet()));
        return users.save(user);
    }

    @Override
    @Transactional
    public void disable(Long id) {
        get(id).setEnabled(false);
    }

    @Override
    @Transactional
    public void enable(Long id) {
        User user = get(id);
        user.setEnabled(true);
        user.setLocked(false);
        user.setFailedLoginAttempts(0);
        user.setLockoutUntil(null);
    }

    @Override
    public List<User> list() {
        return users.findAllByOrderByUsernameAsc();
    }

    @Override
    public List<User> borrowers() {
        return users.findAllByOrderByUsernameAsc().stream()
                .filter(User::isEnabled)
                .toList();
    }

    @Override
    public List<Parent> parents() {
        return parents.findAllByOrderByLastNameAscFirstNameAsc();
    }

    @Override
    public List<RoleUsage> roleUsage() {
        List<RoleUsage> usage = new ArrayList<>();
        for (UserRoleType type : UserRoleType.values()) {
            String description = roles.findByName(type).map(Role::getDescription).orElse("Not configured");
            usage.add(new RoleUsage(type, description, users.findByRolesName(type).size()));
        }
        return usage;
    }
}
