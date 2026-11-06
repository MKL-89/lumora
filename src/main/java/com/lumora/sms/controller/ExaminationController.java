package com.lumora.sms.controller;

import com.lumora.sms.dto.MarkEntryRequest;
import com.lumora.sms.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.math.BigDecimal;
import java.util.Map;

/** Examination setup, subject mark sheets, and result publication. */
@Controller
@RequiredArgsConstructor
public class ExaminationController {

    private final ExaminationService examinations;
    private final AcademicService academics;

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
                            Model model) {
        var examination = examinations.get(examinationId);
        var subjects = examinations.subjectsOf(examinationId);

        model.addAttribute("module", "Mark entry");
        model.addAttribute("examination", examination);
        model.addAttribute("subjects", subjects);
        model.addAttribute("teachers", academics.teachers());

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
                            @RequestParam Long teacherId,
                            @RequestParam Map<String, String> allParams,
                            RedirectAttributes flash) {
        int saved = 0;
        for (Map.Entry<String, String> entry : allParams.entrySet()) {
            if (!entry.getKey().startsWith("mark_") || entry.getValue() == null || entry.getValue().isBlank()) {
                continue;
            }
            Long studentId = Long.valueOf(entry.getKey().substring("mark_".length()));
            examinations.enterMark(new MarkEntryRequest(
                    teacherId, examSubjectId, studentId, new BigDecimal(entry.getValue().trim()), null));
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

    @GetMapping("/results")
    public String results(Model model) {
        model.addAttribute("module", "Results analysis");
        model.addAttribute("examinations", examinations.list());
        model.addAttribute("performance", examinations.averageBySubject(null));
        model.addAttribute("grades", examinations.gradeDistribution());
        return "modules/results";
    }
}
