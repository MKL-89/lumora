package com.lumora.sms.controller;

import com.lumora.sms.dto.AnnouncementRequest;
import com.lumora.sms.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.security.Principal;

/** School notices and the personal notification inbox. */
@Controller
@RequiredArgsConstructor
public class CommunicationController {

    private final NotificationService notifications;

    @GetMapping("/announcements")
    public String announcements(Model model) {
        model.addAttribute("module", "Announcements");
        model.addAttribute("items", notifications.announcements());
        return "modules/announcements";
    }

    @PostMapping("/announcements")
    public String publish(@Valid @ModelAttribute("form") AnnouncementRequest form,
                          BindingResult binding,
                          Principal principal,
                          Model model,
                          RedirectAttributes flash) {
        if (binding.hasErrors()) {
            model.addAttribute("module", "Announcements");
            model.addAttribute("items", notifications.announcements());
            model.addAttribute("errors", binding.getAllErrors());
            return "modules/announcements";
        }
        var published = notifications.publish(form, principal.getName());
        flash.addFlashAttribute("message", "Published \"" + published.getTitle() + "\" to the whole school.");
        return "redirect:/announcements";
    }

    @GetMapping("/notifications")
    public String notifications(Principal principal, Model model) {
        model.addAttribute("module", "Notifications");
        model.addAttribute("items", notifications.inboxFor(principal.getName()));
        return "modules/notifications";
    }
}
