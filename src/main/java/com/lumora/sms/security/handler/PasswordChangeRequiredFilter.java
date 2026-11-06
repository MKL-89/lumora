package com.lumora.sms.security.handler;

import com.lumora.sms.security.AccountSecurityStateService;
import com.lumora.sms.security.LumoraUserPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.List;

/**
 * Someone signed in with a temporary password (one an administrator set) may only
 * reach the change-password page, sign out, and the static files those pages
 * need. Every other request is sent back to the change-password page until they
 * have chosen their own password.
 *
 * <p>The answer is looked up once per session and kept there, so the check costs
 * one query per sign-in rather than one per request. It covers every way of
 * signing in, remember-me included. A successful change clears it.
 */
public class PasswordChangeRequiredFilter extends OncePerRequestFilter {

    public static final String SESSION_FLAG = "lumora.mustChangePassword";
    public static final String CHANGE_PAGE = "/account/password";

    private static final List<String> ALLOWED_PREFIXES = List.of(
            CHANGE_PAGE, "/logout", "/css/", "/js/", "/images/", "/media/", "/error", "/favicon.ico");

    private final AccountSecurityStateService accountState;

    public PasswordChangeRequiredFilter(AccountSecurityStateService accountState) {
        this.accountState = accountState;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof LumoraUserPrincipal principal) {
            HttpSession session = request.getSession();
            Object flag = session.getAttribute(SESSION_FLAG);
            if (flag == null) {
                flag = accountState.mustChangePassword(principal.id());
                session.setAttribute(SESSION_FLAG, flag);
            }
            if (Boolean.TRUE.equals(flag) && !allowed(request)) {
                response.sendRedirect(response.encodeRedirectURL(request.getContextPath() + CHANGE_PAGE));
                return;
            }
        }
        chain.doFilter(request, response);
    }

    static boolean allowed(HttpServletRequest request) {
        // Strip the ";jsessionid=..." that per-tab sessions put into the path.
        String path = request.getRequestURI().substring(request.getContextPath().length());
        int semicolon = path.indexOf(';');
        if (semicolon >= 0) {
            path = path.substring(0, semicolon);
        }
        for (String prefix : ALLOWED_PREFIXES) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}
