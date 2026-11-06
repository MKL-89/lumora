package com.lumora.sms.security;

import com.lumora.sms.entity.Teacher;
import com.lumora.sms.service.AcademicService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Decides which teacher a mark or attendance entry is recorded against.
 *
 * <p>Forms and the REST API carry a {@code teacherId}, and the subject-assignment
 * rule is checked against it. If that id were trusted, a teacher could name a
 * colleague and enter marks for a subject they do not teach. So a signed-in
 * teacher is always recorded as themselves, whatever the request says. School
 * leadership may still record on a teacher's behalf, and the assignment rule is
 * then checked against the teacher they chose.
 */
@Component
@RequiredArgsConstructor
public class ActingTeacher {

    private static final Set<String> LEADERSHIP = Set.of(
            "ROLE_SUPER_ADMIN", "ROLE_ADMINISTRATOR", "ROLE_PRINCIPAL", "ROLE_DEPUTY_PRINCIPAL");
    private static final Set<String> TEACHING = Set.of("ROLE_TEACHER", "ROLE_CLASS_TEACHER");

    private final AcademicService academics;

    /** The teacher this user is limited to, or empty when they may act for any teacher. */
    public Optional<Teacher> restrictedTo(Authentication authentication) {
        if (holdsAny(authentication, LEADERSHIP) || !holdsAny(authentication, TEACHING)) {
            return Optional.empty();
        }
        return Optional.of(academics.teacherForUser(authentication.getName())
                .orElseThrow(() -> new AccessDeniedException(
                        "This account is not linked to a teacher record, so it cannot record marks or attendance.")));
    }

    /** The teacher id to record against: the user's own when they are a teacher. */
    public Long resolve(Authentication authentication, Long requested) {
        return restrictedTo(authentication).map(Teacher::getId).orElse(requested);
    }

    /** The teachers this user may pick from on a form. */
    public List<Teacher> choices(Authentication authentication) {
        return restrictedTo(authentication).map(List::of).orElseGet(academics::teachers);
    }

    private static boolean holdsAny(Authentication authentication, Set<String> roles) {
        return authentication != null && authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).anyMatch(roles::contains);
    }
}
