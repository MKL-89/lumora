package com.lumora.sms.controller;

import com.lumora.sms.service.AcademicService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/** Class, stream, subject, and calendar administration screens. */
@Controller
@RequiredArgsConstructor
public class AcademicController {

    private final AcademicService academics;

    @GetMapping("/subjects")
    public String subjects(Model model) {
        model.addAttribute("module", "Subjects");
        model.addAttribute("items", academics.subjects());
        return "modules/subjects";
    }

    @GetMapping("/classes")
    public String classes(Model model) {
        model.addAttribute("module", "Classes and streams");
        model.addAttribute("classes", academics.classes());
        model.addAttribute("streams", academics.streams());
        return "modules/classes";
    }

    @GetMapping("/timetable")
    public String timetable(Model model) {
        model.addAttribute("module", "Teaching allocation");
        model.addAttribute("streams", academics.streams());
        model.addAttribute("terms", academics.terms());
        model.addAttribute("activeTerm", academics.activeTerm());
        model.addAttribute("years", academics.academicYears());
        return "modules/timetable";
    }
}
