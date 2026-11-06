package com.lumora.sms.controller;

import com.lumora.sms.dto.MarkEntryRequest;
import com.lumora.sms.entity.ExamResult;
import com.lumora.sms.entity.Examination;
import com.lumora.sms.entity.Student;
import com.lumora.sms.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/** Examination setup, subject mark sheets, and result publication. */
@Controller
@RequiredArgsConstructor
public class ExaminationController {

    private final ExaminationService examinations;
    private final AcademicService academics;
    private final StudentService students;
    private final DashboardAccessService access;
    private final com.lumora.sms.security.ActingTeacher actingTeacher;

    @GetMapping("/examinations")
    public String examinations(Model model) {
        model.addAttribute("module", "Examinations");
        model.addAttribute("items", examinations.list());
        return "modules/examinations";
    }

    /** The mark sheet for one paper, listing every candidate in the exam's stream. */
    @GetMapping("/examinations/{examinationId}")
    public String marksheet(@PathVariable Long examinationId,
                            @RequestParam(required = false) Long examSubjectId,
                            Authentication authentication,
                            Model model) {
        var examination = examinations.get(examinationId);
        var subjects = examinations.subjectsOf(examinationId);

        // Deputy principals and administrators may read mark sheets but not enter
        // marks; published sheets are locked for everyone.
        boolean entersMarks = has(authentication, "ROLE_PRINCIPAL", "ROLE_TEACHER", "ROLE_CLASS_TEACHER");
        model.addAttribute("readOnly", examination.isPublished() || !entersMarks);
        model.addAttribute("viewOnlyRole", !entersMarks);

        model.addAttribute("module", "Mark entry");
        model.addAttribute("examination", examination);
        model.addAttribute("subjects", subjects);
        model.addAttribute("teachers", actingTeacher.choices(authentication));
        model.addAttribute("ownTeacherId",
                actingTeacher.restrictedTo(authentication).map(t -> t.getId()).orElse(null));

        Long selected = examSubjectId != null ? examSubjectId
                : subjects.isEmpty() ? null : subjects.get(0).getId();
        if (selected != null) {
            model.addAttribute("examSubject", examinations.examSubject(selected));
            model.addAttribute("rows", examinations.markSheet(selected));
        }
        return "modules/marksheet";
    }

    /**
     * Saves a whole mark sheet. Each mark goes through the service one at a time so
     * the teacher-assignment and maximum-mark rules apply to every entry.
     */
    @PostMapping("/examinations/{examinationId}/marks")
    public String saveMarks(@PathVariable Long examinationId,
                            @RequestParam Long examSubjectId,
                            @RequestParam(required = false) Long teacherId,
                            @RequestParam Map<String, String> allParams,
                            Authentication authentication,
                            RedirectAttributes flash) {
        // A teacher is always recorded as themselves, never as a colleague.
        Long recordedBy = actingTeacher.resolve(authentication, teacherId);
        if (recordedBy == null) {
            flash.addFlashAttribute("problem", "Choose the subject teacher the marks are being entered for.");
            return "redirect:/examinations/" + examinationId + "?examSubjectId=" + examSubjectId;
        }
        int saved = 0;
        for (Map.Entry<String, String> entry : allParams.entrySet()) {
            if (!entry.getKey().startsWith("mark_") || entry.getValue() == null || entry.getValue().isBlank()) {
                continue;
            }
            Long studentId = Long.valueOf(entry.getKey().substring("mark_".length()));
            examinations.enterMark(new MarkEntryRequest(
                    recordedBy, examSubjectId, studentId, new BigDecimal(entry.getValue().trim()), null));
            saved++;
        }
        flash.addFlashAttribute("message", saved + " mark(s) recorded and graded.");
        return "redirect:/examinations/" + examinationId + "?examSubjectId=" + examSubjectId;
    }

    @PostMapping("/examinations/{examinationId}/publish")
    public String publish(@PathVariable Long examinationId, RedirectAttributes flash) {
        examinations.publish(examinationId);
        flash.addFlashAttribute("message", "Results published. Marks are now locked.");
        return "redirect:/examinations";
    }

    /**
     * Results lookup. Nothing is shown until an admission number is entered.
     * Teachers and the principal may look up any learner; a learner only
     * themselves, and a guardian only their own children. A number the user may
     * not see gets the same answer as one that does not exist, so the page cannot
     * be used to discover who is enrolled.
     */
    @GetMapping("/results")
    public String results(@RequestParam(value = "adm", required = false) String adm,
                          Authentication authentication,
                          Model model) {
        String wanted = adm == null ? "" : adm.trim();
        model.addAttribute("module", "Results");
        model.addAttribute("adm", wanted);
        model.addAttribute("ownOnly", !isStaff(authentication));
        if (has(authentication, "ROLE_PARENT")) {
            model.addAttribute("children", access.childrenOf(authentication.getName()));
        }
        if (wanted.isEmpty()) {
            return "modules/results";
        }

        Optional<Student> learner = students.findByAdmissionNumber(wanted)
                .filter(s -> maySee(authentication, s.getId()));
        if (learner.isEmpty()) {
            model.addAttribute("notFound", true);
            return "modules/results";
        }

        List<ExamResult> rows = examinations.publishedResultsFor(learner.get().getId());
        model.addAttribute("profile", students.profile(learner.get().getId()));
        model.addAttribute("results", rows);
        model.addAttribute("summaries", summarise(rows));
        return "modules/results";
    }

    private boolean maySee(Authentication authentication, Long studentId) {
        if (isStaff(authentication)) {
            return true;
        }
        try {
            if (has(authentication, "ROLE_STUDENT")) {
                access.assertStudentOwnsRecord(authentication.getName(), studentId);
                return true;
            }
            if (has(authentication, "ROLE_PARENT")) {
                access.assertParentOwnsStudent(authentication.getName(), studentId);
                return true;
            }
        } catch (AccessDeniedException refused) {
            return false;
        }
        return false;
    }

    private boolean isStaff(Authentication authentication) {
        return has(authentication, "ROLE_PRINCIPAL", "ROLE_DEPUTY_PRINCIPAL", "ROLE_ADMINISTRATOR",
                "ROLE_TEACHER", "ROLE_CLASS_TEACHER");
    }

    private static boolean has(Authentication authentication, String... roles) {
        Set<String> held = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).collect(Collectors.toSet());
        return Arrays.stream(roles).anyMatch(held::contains);
    }

    /** One line per examination: how many papers, mean percentage and mean points. */
    static List<ExamSummary> summarise(List<ExamResult> rows) {
        Map<Long, List<ExamResult>> byExam = new LinkedHashMap<>();
        for (ExamResult r : rows) {
            byExam.computeIfAbsent(r.getExamSubject().getExamination().getId(), k -> new ArrayList<>()).add(r);
        }
        List<ExamSummary> out = new ArrayList<>();
        byExam.forEach((examId, results) -> {
            Examination exam = results.get(0).getExamSubject().getExamination();
            BigDecimal percentTotal = BigDecimal.ZERO;
            int pointsTotal = 0;
            int pointsCount = 0;
            for (ExamResult r : results) {
                BigDecimal max = r.getExamSubject().getMaximumMark();
                if (max != null && max.signum() > 0) {
                    percentTotal = percentTotal.add(r.getMarks().multiply(BigDecimal.valueOf(100))
                            .divide(max, 4, RoundingMode.HALF_UP));
                }
                if (r.getPoints() != null) {
                    pointsTotal += r.getPoints();
                    pointsCount++;
                }
            }
            BigDecimal meanPercent = percentTotal.divide(BigDecimal.valueOf(results.size()), 1, RoundingMode.HALF_UP);
            BigDecimal meanPoints = pointsCount == 0 ? null
                    : BigDecimal.valueOf(pointsTotal).divide(BigDecimal.valueOf(pointsCount), 1, RoundingMode.HALF_UP);
            out.add(new ExamSummary(exam.getName(), exam.getExaminationDate(), results.size(),
                    meanPercent, pointsTotal, meanPoints));
        });
        return out;
    }

    public record ExamSummary(String examination, LocalDate date, int papers,
                              BigDecimal meanPercent, int totalPoints, BigDecimal meanPoints) { }
}
