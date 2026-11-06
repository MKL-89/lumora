package com.lumora.sms.security.handler;

import com.lumora.sms.security.LumoraUserPrincipal;
import com.lumora.sms.security.audit.SecurityAuditService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.*;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.util.*;

/** Clears temporary lockout state, audits the sign-in, and selects a role dashboard. */
@Component
@RequiredArgsConstructor
public class DashboardAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    /**
     * Roles in descending order of scope. The first one a user holds decides the
     * dashboard they land on, so an account with several roles always gets the
     * broadest view it is entitled to.
     */
    private static final List<Map.Entry<String, String>> ROLE_DESTINATIONS = List.of(
            Map.entry("ROLE_SUPER_ADMIN", "/dashboard/super-admin"),
            Map.entry("ROLE_ADMINISTRATOR", "/dashboard/administrator"),
            Map.entry("ROLE_PRINCIPAL", "/dashboard/principal"),
            Map.entry("ROLE_DEPUTY_PRINCIPAL", "/dashboard/deputy-principal"),
            Map.entry("ROLE_BURSAR", "/dashboard/bursar"),
            Map.entry("ROLE_REGISTRAR", "/dashboard/registrar"),
            Map.entry("ROLE_CLASS_TEACHER", "/dashboard/class-teacher"),
            Map.entry("ROLE_TEACHER", "/dashboard/teacher"),
            Map.entry("ROLE_LIBRARIAN", "/dashboard/librarian"),
            Map.entry("ROLE_PARENT", "/dashboard/parent"),
            Map.entry("ROLE_STUDENT", "/dashboard/student"));

    private final SecurityAuditService audit;
    private final com.lumora.sms.security.AccountSecurityStateService accountState;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest req, HttpServletResponse res, Authentication auth)
            throws IOException, ServletException {
        LumoraUserPrincipal principal = (LumoraUserPrincipal) auth.getPrincipal();
        accountState.recordSuccess(principal.id());
        audit.record(principal.username(), "LOGIN_SUCCESS", req.getRemoteAddr());

        // An account created or reset by an administrator carries a temporary
        // password. Until its holder chooses their own, PasswordChangeRequiredFilter
        // keeps them on the change-password page.
        String destination = destinationFor(auth.getAuthorities());
        boolean mustChange = accountState.mustChangePassword(principal.id());
        req.getSession().setAttribute(PasswordChangeRequiredFilter.SESSION_FLAG, mustChange);
        if (mustChange) {
            destination = PasswordChangeRequiredFilter.CHANGE_PAGE;
        }

        // Session fixation protection has just issued a new session id. Encoding the
        // redirect carries that new id into this tab's URL, so the tab stays signed
        // in without a shared cookie. Under cookie tracking this is a no-op.
        res.sendRedirect(res.encodeRedirectURL(req.getContextPath() + destination));
    }

    /** The dashboard a set of authorities should land on. Also used by the root URL. */
    public String destinationFor(Collection<? extends GrantedAuthority> authorities) {
        Set<String> held = new HashSet<>();
        authorities.forEach(authority -> held.add(authority.getAuthority()));

        for (Map.Entry<String, String> candidate : ROLE_DESTINATIONS) {
            if (held.contains(candidate.getKey())) {
                return candidate.getValue();
            }
        }
        return "/dashboard/student";
    }
}
