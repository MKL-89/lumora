package com.lumora.sms.controller;

import com.lumora.sms.dto.AccountCreateRequest;
import com.lumora.sms.enums.UserRoleType;
import com.lumora.sms.exception.BusinessRuleViolationException;
import com.lumora.sms.exception.DuplicateResourceException;
import com.lumora.sms.exception.ResourceNotFoundException;
import com.lumora.sms.security.audit.SecurityAuditService;
import com.lumora.sms.service.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/** User, role, teacher, and guardian administration screens. */
@Controller
@RequiredArgsConstructor
public class AdministrationController {

    private final AcademicService academics;
    private final UserService users;
    private final SecurityAuditService audit;

    @GetMapping("/teachers")
    public String teachers(Model model) {
        model.addAttribute("module", "Teachers");
        model.addAttribute("items", academics.teachers());
        return "modules/teachers";
    }

    @GetMapping("/parents")
    public String parents(Model model) {
        model.addAttribute("module", "Parents and guardians");
        model.addAttribute("items", users.parents());
        return "modules/parents";
    }

    @GetMapping("/users")
    public String users(Model model, Authentication authentication) {
        model.addAttribute("module", "User accounts");
        model.addAttribute("items", users.list());
        boolean superAdmin = isSuperAdmin(authentication);
        model.addAttribute("roles", java.util.Arrays.stream(UserRoleType.values())
                .filter(r -> superAdmin || r != UserRoleType.SUPER_ADMIN).toList());
        return "modules/users";
    }

    /**
     * Creates an account with a generated temporary password. The password is
     * shown to the administrator exactly once, to hand to the person; it is never
     * stored in readable form or written to a log.
     */
    @PostMapping("/users")
    public String createUser(@Valid @ModelAttribute AccountCreateRequest form, BindingResult binding,
                             Authentication authentication, HttpServletRequest request,
                             RedirectAttributes flash) {
        if (binding.hasErrors()) {
            flash.addFlashAttribute("problem", binding.getAllErrors().get(0).getDefaultMessage());
            flash.addFlashAttribute("draft", form);
            return "redirect:/users#new";
        }
        try {
            String temporary = users.createAccount(form, isSuperAdmin(authentication));
            audit.record(authentication.getName(), action("ACCOUNT_CREATED:", form.username().trim()), request.getRemoteAddr());
            flash.addFlashAttribute("issued", new IssuedPassword(form.username().trim(), temporary, true));
        } catch (BusinessRuleViolationException | DuplicateResourceException | ResourceNotFoundException e) {
            flash.addFlashAttribute("problem", e.getMessage());
            flash.addFlashAttribute("draft", form);
            return "redirect:/users#new";
        }
        return "redirect:/users";
    }

    @PostMapping("/users/{id}/reset-password")
    public String resetPassword(@PathVariable Long id, Authentication authentication,
                                HttpServletRequest request, RedirectAttributes flash) {
        try {
            String username = users.get(id).getUsername();
            String temporary = users.resetPassword(id, isSuperAdmin(authentication));
            audit.record(authentication.getName(), action("PASSWORD_RESET:", username), request.getRemoteAddr());
            flash.addFlashAttribute("issued", new IssuedPassword(username, temporary, false));
        } catch (BusinessRuleViolationException e) {
            flash.addFlashAttribute("problem", e.getMessage());
        }
        return "redirect:/users";
    }

    /** Shown once on the accounts page after an account is created or its password reset. */
    public record IssuedPassword(String username, String password, boolean newAccount) {
    }

    /** Audit actions hold at most 80 characters. */
    private static String action(String prefix, String username) {
        String action = prefix + username;
        return action.length() > 80 ? action.substring(0, 80) : action;
    }

    private static boolean isSuperAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_SUPER_ADMIN".equals(a.getAuthority()));
    }

    @GetMapping("/roles")
    public String roles(Model model) {
        model.addAttribute("module", "Roles");
        model.addAttribute("items", users.roleUsage());
        return "modules/roles";
    }
}
