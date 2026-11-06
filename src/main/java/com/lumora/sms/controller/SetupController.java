package com.lumora.sms.controller;

import com.lumora.sms.config.SchoolDefaults;
import com.lumora.sms.dto.SetupRequest;
import com.lumora.sms.exception.BusinessRuleViolationException;
import com.lumora.sms.security.audit.SecurityAuditService;
import com.lumora.sms.service.SetupService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

/**
 * The first-time setup page. It is open to anyone only while the database has no
 * accounts; from the moment the administrator is created it redirects to sign-in.
 */
@Controller
@RequestMapping("/setup")
@RequiredArgsConstructor
public class SetupController {

    private final SetupService setup;
    private final SecurityAuditService audit;

    @GetMapping
    public String form(Model model) {
        if (!setup.needed()) {
            return "redirect:/login";
        }
        LocalDate today = LocalDate.now();
        int term = today.getMonthValue() <= 4 ? 1 : today.getMonthValue() <= 8 ? 2 : 3;
        List<String> usual = SchoolDefaults.KCSE_SUBJECTS.stream()
                .filter(SchoolDefaults.SubjectOption::usual).map(SchoolDefaults.SubjectOption::code).toList();
        model.addAttribute("form", new SetupRequest("", "", "", "", "", today.getYear(), term,
                "East, West", usual, "", "", "", ""));
        return show(model);
    }

    @PostMapping
    public String run(@Valid @ModelAttribute("form") SetupRequest form, BindingResult binding,
                      Model model, HttpServletRequest request) {
        if (!setup.needed()) {
            return "redirect:/login";
        }
        if (binding.hasErrors()) {
            model.addAttribute("errors", binding.getAllErrors());
            return show(model);
        }
        try {
            var admin = setup.run(form);
            audit.record(admin.getUsername(), "FIRST_TIME_SETUP", request.getRemoteAddr());
            return "redirect:/login?setup";
        } catch (BusinessRuleViolationException e) {
            model.addAttribute("problem", e.getMessage());
            return show(model);
        }
    }

    private String show(Model model) {
        model.addAttribute("subjectOptions", SchoolDefaults.KCSE_SUBJECTS);
        return "setup";
    }
}
