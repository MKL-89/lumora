package com.lumora.sms.security.handler;

import com.lumora.sms.security.audit.SecurityAuditService;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;
import java.io.IOException;

/**
 * Audits logout then returns the user to the sign-in page.
 *
 * <p>Only the session behind this request is invalidated, so signing out in one
 * tab leaves every other tab signed in.
 */
@Component
@RequiredArgsConstructor
public class AuditingLogoutSuccessHandler implements LogoutSuccessHandler {

    private final SecurityAuditService audit;

    @Override
    public void onLogoutSuccess(HttpServletRequest req, HttpServletResponse res, Authentication auth)
            throws IOException {
        if (auth != null) {
            audit.record(auth.getName(), "LOGOUT", req.getRemoteAddr());
        }
        // The session is already invalid here, so no id is encoded: the tab lands
        // on a clean sign-in page and starts a fresh session of its own.
        res.sendRedirect(res.encodeRedirectURL(req.getContextPath() + "/login?logout"));
    }
}
