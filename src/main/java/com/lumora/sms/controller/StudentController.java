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
    private final StudentImportService importer;
    private final com.lumora.sms.security.audit.SecurityAuditService audit;

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

    /** Bringing learners across from the school's previous system or a spreadsheet. */
    @GetMapping("/import")
    public String importForm(Model model) {
        model.addAttribute("module", "Import learners");
        model.addAttribute("streams", academics.streams());
        return "modules/student-import";
    }

    @GetMapping(value = "/import/template.csv", produces = "text/csv")
    @ResponseBody
    public org.springframework.http.ResponseEntity<byte[]> importTemplate() {
        byte[] body = importer.templateCsv().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return org.springframework.http.ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"lumora-learners-template.csv\"")
                .contentType(org.springframework.http.MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(body);
    }

    @PostMapping("/import")
    public String importFile(@RequestParam("file") org.springframework.web.multipart.MultipartFile file,
                             @RequestParam(defaultValue = "false") boolean skipExisting,
                             Authentication authentication,
                             jakarta.servlet.http.HttpServletRequest request,
                             Model model) throws java.io.IOException {
        model.addAttribute("module", "Import learners");
        model.addAttribute("streams", academics.streams());
        model.addAttribute("skipExisting", skipExisting);
        if (file == null || file.isEmpty()) {
            model.addAttribute("problem", "Choose the spreadsheet file to upload first.");
            return "modules/student-import";
        }
        if (file.getSize() > 5L * 1024 * 1024) {
            model.addAttribute("problem", "That file is larger than 5 MB. Split it into smaller files.");
            return "modules/student-import";
        }
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        if (name.endsWith(".xlsx") || name.endsWith(".xls")) {
            model.addAttribute("problem", "That is an Excel workbook. In Excel choose File, Save As, and pick "
                    + "\"CSV UTF-8 (Comma delimited)\", then upload the .csv file.");
            return "modules/student-import";
        }
        try {
            var result = importer.importCsv(file.getBytes(), skipExisting);
            model.addAttribute("result", result);
            if (result.ok()) {
                audit.record(authentication.getName(), "STUDENTS_IMPORTED:" + result.imported(),
                        request.getRemoteAddr());
            }
        } catch (com.lumora.sms.exception.BusinessRuleViolationException e) {
            model.addAttribute("problem", e.getMessage());
        }
        return "modules/student-import";
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

        // Results are for teachers, the principal, the deputy and administrators
        // (every mark, drafts included)
        // and for the learner and their guardians (published examinations only).
        // Every other role sees the record without results.
        if (hasAny(authentication, "ROLE_PRINCIPAL", "ROLE_DEPUTY_PRINCIPAL", "ROLE_ADMINISTRATOR",
                "ROLE_TEACHER", "ROLE_CLASS_TEACHER")) {
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
