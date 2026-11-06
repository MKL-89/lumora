package com.lumora.sms.controller;

import com.lumora.sms.dto.StudentAdmissionRequest;
import com.lumora.sms.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/** Renders the student list, the admission form, and individual profiles. */
@Controller
@RequestMapping("/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService students;
    private final AcademicService academics;
    private final DashboardAccessService access;
    private final ExaminationService examinations;

    @GetMapping
    public String list(@RequestParam(required = false) String q, Model model) {
        model.addAttribute("items", students.search(q));
        model.addAttribute("query", q);
        model.addAttribute("module", "Students");
        return "modules/students";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        addFormOptions(model);
        return "modules/student-form";
    }

    @PostMapping
    public String admit(@Valid @ModelAttribute("form") StudentAdmissionRequest form,
                        BindingResult binding,
                        Model model,
                        RedirectAttributes flash) {
        if (binding.hasErrors()) {
            addFormOptions(model);
            model.addAttribute("errors", binding.getAllErrors());
            return "modules/student-form";
        }
        var admitted = students.admit(form);
        flash.addFlashAttribute("message",
                "Admitted " + admitted.firstName() + " " + admitted.lastName()
                        + " (" + admitted.admissionNumber() + ").");
        return "redirect:/students/" + admitted.id();
    }

    /**
     * Staff see any learner. A signed-in learner or guardian must prove ownership
     * of the record first.
     *
     * <p>Which check applies is decided from the authenticated authorities, never
     * from a request parameter — otherwise a guardian could simply omit the
     * parameter and read any learner in the school.
     */
    @GetMapping("/{id}")
    public String view(@PathVariable Long id, Authentication authentication, Model model) {
        if (!hasAny(authentication, STAFF_ROLES)) {
            if (hasAny(authentication, "ROLE_STUDENT")) {
                access.assertStudentOwnsRecord(authentication.getName(), id);
            } else if (hasAny(authentication, "ROLE_PARENT")) {
                access.assertParentOwnsStudent(authentication.getName(), id);
            } else {
                throw new AccessDeniedException("This account cannot view learner records.");
            }
        }
        var profile = students.profile(id);
        model.addAttribute("profile", profile);

        // Results are for teachers and the principal (every mark, drafts included)
        // and for the learner and their guardians (published examinations only).
        // Every other role sees the record without results.
        if (hasAny(authentication, "ROLE_PRINCIPAL", "ROLE_TEACHER", "ROLE_CLASS_TEACHER")) {
            model.addAttribute("results", profile.results());
            model.addAttribute("resultsPublishedOnly", false);
        } else if (hasAny(authentication, "ROLE_STUDENT", "ROLE_PARENT")) {
            model.addAttribute("results", examinations.publishedResultsFor(id));
            model.addAttribute("resultsPublishedOnly", true);
        }
        return "modules/student-view";
    }

    /** Roles entitled to see any learner in the school. */
    private static final String[] STAFF_ROLES = {
            "ROLE_SUPER_ADMIN", "ROLE_ADMINISTRATOR", "ROLE_PRINCIPAL", "ROLE_DEPUTY_PRINCIPAL",
            "ROLE_REGISTRAR", "ROLE_BURSAR", "ROLE_TEACHER", "ROLE_CLASS_TEACHER"};

    private boolean hasAny(Authentication authentication, String... roles) {
        Set<String> held = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
        return Arrays.stream(roles).anyMatch(held::contains);
    }

    @PostMapping("/{id}/deactivate")
    public String deactivate(@PathVariable Long id, RedirectAttributes flash) {
        students.deactivate(id);
        flash.addFlashAttribute("message", "The learner record was deactivated.");
        return "redirect:/students/" + id;
    }

    @PostMapping("/{id}/reactivate")
    public String reactivate(@PathVariable Long id, RedirectAttributes flash) {
        students.reactivate(id);
        flash.addFlashAttribute("message", "The learner record was reactivated.");
        return "redirect:/students/" + id;
    }

    private void addFormOptions(Model model) {
        model.addAttribute("module", "Admit student");
        model.addAttribute("streams", academics.streams());
        model.addAttribute("years", academics.academicYears());
        model.addAttribute("genders", com.lumora.sms.enums.Gender.values());
    }
}
