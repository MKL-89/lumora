package com.lumora.sms.web;

import com.lumora.sms.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * How the signed-in person is shown in the top bar and greetings. Exposed to
 * Thymeleaf as {@code @people}, so templates show "Mercy Chebet · Teacher"
 * instead of a username and a raw authority such as {@code [ROLE_TEACHER]}.
 */
@Component("people")
@RequiredArgsConstructor
public class People {

    /** Readable names for each role, in the order they are listed when a user has several. */
    private static final Map<String, String> ROLE_LABELS = new java.util.LinkedHashMap<>();
    static {
        ROLE_LABELS.put("ROLE_SUPER_ADMIN", "Super administrator");
        ROLE_LABELS.put("ROLE_ADMINISTRATOR", "Administrator");
        ROLE_LABELS.put("ROLE_PRINCIPAL", "Principal");
        ROLE_LABELS.put("ROLE_DEPUTY_PRINCIPAL", "Deputy principal");
        ROLE_LABELS.put("ROLE_REGISTRAR", "Registrar");
        ROLE_LABELS.put("ROLE_BURSAR", "Bursar");
        ROLE_LABELS.put("ROLE_CLASS_TEACHER", "Class teacher");
        ROLE_LABELS.put("ROLE_TEACHER", "Teacher");
        ROLE_LABELS.put("ROLE_LIBRARIAN", "Librarian");
        ROLE_LABELS.put("ROLE_STUDENT", "Student");
        ROLE_LABELS.put("ROLE_PARENT", "Parent / guardian");
    }

    private final UserService users;

    public String name(Authentication authentication) {
        return authentication == null ? "" : users.displayName(authentication.getName());
    }

    /** Up to two initials for the avatar: "Mercy Chebet" gives "MC". */
    public String initials(Authentication authentication) {
        String name = name(authentication).trim();
        if (name.isEmpty()) {
            return "?";
        }
        String[] parts = name.split("\\s+");
        String first = parts[0].substring(0, 1);
        String last = parts.length > 1 ? parts[parts.length - 1].substring(0, 1) : "";
        return (first + last).toUpperCase();
    }

    /** "Teacher", or "Principal, Teacher" for someone with two roles. */
    public String role(Authentication authentication) {
        if (authentication == null) {
            return "";
        }
        List<String> held = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).toList();
        return ROLE_LABELS.entrySet().stream()
                .filter(e -> held.contains(e.getKey()))
                .map(Map.Entry::getValue)
                .collect(Collectors.joining(", "));
    }
}
