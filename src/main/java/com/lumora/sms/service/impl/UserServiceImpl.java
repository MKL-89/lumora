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
import java.util.List;
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
