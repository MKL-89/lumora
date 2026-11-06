package com.lumora.sms.controller;

import com.lumora.sms.exception.BusinessRuleViolationException;
import com.lumora.sms.security.audit.SecurityAuditService;
import com.lumora.sms.security.handler.PasswordChangeRequiredFilter;
import com.lumora.sms.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Every signed-in person's own account: for now, changing their password. */
@Controller
@RequestMapping("/account")
@RequiredArgsConstructor
public class AccountController {

    private final UserService users;
    private final SecurityAuditService audit;
    private final com.lumora.sms.security.handler.DashboardAuthenticationSuccessHandler destinations;

    @GetMapping("/password")
    public String passwordForm(HttpSession session, Model model) {
        model.addAttribute("module", "Change password");
        model.addAttribute("mustChange",
                Boolean.TRUE.equals(session.getAttribute(PasswordChangeRequiredFilter.SESSION_FLAG)));
        return "modules/account-password";
    }

    @PostMapping("/password")
    public String changePassword(@RequestParam String currentPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 Authentication authentication,
                                 HttpServletRequest request,
                                 RedirectAttributes flash) {
        try {
            users.changeOwnPassword(authentication.getName(), currentPassword, newPassword, confirmPassword);
        } catch (BusinessRuleViolationException e) {
            flash.addFlashAttribute("problem", e.getMessage());
            return "redirect:/account/password";
        }
        request.getSession().setAttribute(PasswordChangeRequiredFilter.SESSION_FLAG, Boolean.FALSE);
        audit.record(authentication.getName(), "PASSWORD_CHANGED", request.getRemoteAddr());
        flash.addFlashAttribute("message", "Your password has been changed.");
        // Straight to the dashboard: a second redirect through /home would drop the message.
        return "redirect:" + destinations.destinationFor(authentication.getAuthorities());
    }
}
