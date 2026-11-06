package com.lumora.sms.controller;

import com.lumora.sms.security.handler.DashboardAuthenticationSuccessHandler;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Entry points that sit outside a module: the sign-in screen and the root URL,
 * which forwards each user to the dashboard their role owns.
 */
@Controller
@RequiredArgsConstructor
public class HomeController {

    private final DashboardAuthenticationSuccessHandler destinations;

    /**
     * Sends each user to the dashboard their role owns.
     *
     * <p>{@code /home} exists alongside {@code /} because a bare root link rewritten
     * for URL session tracking becomes {@code /;jsessionid=...}, which the container
     * does not resolve back to "/". Templates link to {@code /home} so the same
     * markup works whichever session mode is active.
     */
    @GetMapping({"/", "", "/home"})
    public String root() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return "redirect:/login";
        }
        return "redirect:" + destinations.destinationFor(auth.getAuthorities());
    }

    @GetMapping("/login")
    public String login(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()
                && !"anonymousUser".equals(String.valueOf(auth.getPrincipal()))) {
            return "redirect:/";
        }

        // Create the session before the page renders. Under URL-based tracking the
        // container will only rewrite links once a session exists, and this tab's
        // sign-in form has to carry its own session id or the POST lands nowhere.
        request.getSession(true);
        return "login";
    }
}
