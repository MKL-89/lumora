package com.lumora.sms.security.handler;

import com.lumora.sms.security.AccountSecurityStateService;
import com.lumora.sms.security.audit.SecurityAuditService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import java.io.IOException;

/** Tracks failed credentials and locks a known account after the configured threshold. */
@Component
@RequiredArgsConstructor
public class AccountLockoutFailureHandler implements AuthenticationFailureHandler {

    private final AccountSecurityStateService accountState;
    private final SecurityAuditService audit;

    @Override
    public void onAuthenticationFailure(HttpServletRequest req, HttpServletResponse res,
                                        AuthenticationException ex) throws IOException, ServletException {
        String username = req.getParameter("username");
        accountState.recordFailure(username);
        audit.record(username, "LOGIN_FAILURE", req.getRemoteAddr());

        // encodeRedirectURL keeps this tab's session id in the URL when session
        // tracking is URL-based; it is a no-op under cookie tracking.
        res.sendRedirect(res.encodeRedirectURL(req.getContextPath() + "/login?error"));
    }
}
