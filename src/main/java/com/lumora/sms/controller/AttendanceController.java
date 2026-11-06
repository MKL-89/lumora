package com.lumora.sms.controller;

import com.lumora.sms.enums.AttendanceStatus;
import com.lumora.sms.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.time.LocalDate;
import java.util.*;

/** The daily attendance register: choose a stream and date, then mark the roll. */
@Controller
@RequestMapping("/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendance;
    private final AcademicService academics;

    @GetMapping
    public String register(@RequestParam(required = false) Long streamId,
                           @RequestParam(required = false) Long teacherId,
                           @RequestParam(required = false)
                           @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                           Model model) {
        LocalDate day = date == null ? LocalDate.now() : date;

        model.addAttribute("module", "Attendance");
        model.addAttribute("streams", academics.streams());
        model.addAttribute("teachers", academics.teachers());
        model.addAttribute("statuses", AttendanceStatus.values());
        model.addAttribute("selectedStream", streamId);
        model.addAttribute("selectedTeacher", teacherId);
        model.addAttribute("date", day);
        model.addAttribute("today", LocalDate.now());

        if (streamId != null) {
            model.addAttribute("rows", attendance.register(streamId, day));
        }
        return "modules/attendance";
    }

    /**
     * The form posts one {@code status_{studentId}} field per learner. Blank
     * selections are dropped so a partially completed register still saves.
     */
    @PostMapping
    public String save(@RequestParam Long streamId,
                       @RequestParam Long teacherId,
                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                       @RequestParam Map<String, String> allParams,
                       RedirectAttributes flash) {
        Map<Long, AttendanceStatus> marks = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : allParams.entrySet()) {
            if (!entry.getKey().startsWith("status_") || entry.getValue() == null || entry.getValue().isBlank()) {
                continue;
            }
            Long studentId = Long.valueOf(entry.getKey().substring("status_".length()));
            marks.put(studentId, AttendanceStatus.valueOf(entry.getValue()));
        }

        int saved = attendance.saveRegister(streamId, teacherId, date, marks);
        flash.addFlashAttribute("message", "Register saved for " + saved + " learner(s).");
        return "redirect:/attendance?streamId=" + streamId + "&teacherId=" + teacherId + "&date=" + date;
    }
}
