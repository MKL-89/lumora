package com.lumora.sms.controller;

import com.lumora.sms.service.*;
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
    public String users(Model model) {
        model.addAttribute("module", "User accounts");
        model.addAttribute("items", users.list());
        model.addAttribute("roles", com.lumora.sms.enums.UserRoleType.values());
        return "modules/users";
    }

    @GetMapping("/roles")
    public String roles(Model model) {
        model.addAttribute("module", "Roles");
        model.addAttribute("items", users.roleUsage());
        return "modules/roles";
    }
}
