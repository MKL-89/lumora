package com.lumora.sms.controller;

import com.lumora.sms.exception.BusinessRuleViolationException;
import com.lumora.sms.security.audit.SecurityAuditService;
import com.lumora.sms.web.SchoolMedia;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.Duration;

/**
 * The school's own pictures. Anyone may view them (the sign-in page needs the logo
 * and photo before anyone is signed in); only administrators may change them.
 */
@Controller
@RequiredArgsConstructor
public class BrandingController {

    private final SchoolMedia media;
    private final SecurityAuditService audit;

    @GetMapping("/media/{slot}")
    @ResponseBody
    public ResponseEntity<Resource> picture(@PathVariable String slot) {
        return media.load(slot)
                .map(image -> ResponseEntity.ok()
                        .contentType(image.type())
                        // Pages add ?v=<last change> to the URL, so a long cache is safe.
                        .cacheControl(CacheControl.maxAge(Duration.ofDays(7)).cachePublic())
                        .body(image.resource()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/settings/branding")
    public String page(Model model) {
        model.addAttribute("module", "School photos");
        model.addAttribute("slots", media.slots());
        return "modules/branding";
    }

    @PostMapping("/settings/branding/{slot}")
    public String upload(@PathVariable String slot,
                         @RequestParam(value = "file", required = false) MultipartFile file,
                         Principal principal,
                         HttpServletRequest request,
                         RedirectAttributes flash) {
        try {
            media.store(slot, file);
            audit.record(principal.getName(), "BRANDING_UPDATED:" + slot, request.getRemoteAddr());
            flash.addFlashAttribute("message", "Picture updated. Everyone sees the new one straight away.");
        } catch (BusinessRuleViolationException e) {
            flash.addFlashAttribute("problem", e.getMessage());
        }
        return "redirect:/settings/branding#" + slot;
    }

    @PostMapping("/settings/branding/{slot}/reset")
    public String reset(@PathVariable String slot,
                        Principal principal,
                        HttpServletRequest request,
                        RedirectAttributes flash) {
        try {
            media.reset(slot);
            audit.record(principal.getName(), "BRANDING_RESET:" + slot, request.getRemoteAddr());
            boolean hasDefault = SchoolMedia.Slot.of(slot).map(s -> s.hasDefault).orElse(false);
            flash.addFlashAttribute("message", hasDefault
                    ? "Picture removed. The built-in one is showing again."
                    : "Logo removed. The letter mark is showing again.");
        } catch (BusinessRuleViolationException e) {
            flash.addFlashAttribute("problem", e.getMessage());
        }
        return "redirect:/settings/branding#" + slot;
    }
}
