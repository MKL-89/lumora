package com.lumora.sms.controller;

import com.lumora.sms.service.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;

/**
 * Serves the eleven role-specific dashboard routes protected by SecurityConfig.
 *
 * <p>Every role shares one template but receives a different model: whole-school
 * figures for staff, and ownership-scoped figures for learners and guardians.
 */
@Controller
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardMetricsService metrics;
    private final NotificationService notifications;
    private final StudentService students;
    private final ExaminationService examinations;
    private final LibraryService library;
    private final DashboardAccessService access;

    @GetMapping({"/super-admin", "/administrator", "/principal", "/deputy-principal", "/registrar",
                 "/teacher", "/class-teacher", "/bursar", "/librarian"})
    public String staffDashboard(Principal principal, Model model, HttpServletRequest request) {
        String role = roleSegment(request);
        addCommon(model, principal, role);

        model.addAttribute("summary", metrics.summary());
        model.addAttribute("enrolmentChart", metrics.enrolmentByClass());
        model.addAttribute("performanceChart", metrics.subjectPerformance());
        model.addAttribute("collectionChart", metrics.feeCollectionByMethod());
        model.addAttribute("attendanceChart", metrics.attendanceTrend(14));
        model.addAttribute("activity", metrics.recentActivity());
        model.addAttribute("overdueLoans", library.overdueLoans());
        return "dashboard/index";
    }

    /**
     * A learner sees only their own record. The student identity comes from the
     * session, never from a request parameter, so one learner cannot request
     * another's dashboard.
     */
    @GetMapping("/student")
    public String studentDashboard(Principal principal, Model model, HttpServletRequest request) {
        addCommon(model, principal, roleSegment(request));

        var student = students.requireByUsername(principal.getName());
        access.assertStudentOwnsRecord(principal.getName(), student.getId());

        model.addAttribute("profile", students.profile(student.getId()));
        model.addAttribute("results", examinations.publishedResultsFor(student.getId()));
        return "dashboard/student";
    }

    /** A guardian sees only the children explicitly linked to their account. */
    @GetMapping("/parent")
    public String parentDashboard(Principal principal, Model model, HttpServletRequest request) {
        addCommon(model, principal, roleSegment(request));

        var profiles = access.childrenOf(principal.getName()).stream()
                .map(child -> students.profile(child.getId()))
                .toList();
        model.addAttribute("children", profiles);
        return "dashboard/parent";
    }

    private void addCommon(Model model, Principal principal, String role) {
        model.addAttribute("dashboardRole", role.replace('-', ' ').toUpperCase());
        model.addAttribute("username", principal.getName());
        model.addAttribute("announcements", notifications.currentAnnouncements());
    }

    /**
     * The dashboard name is the last path segment. Any path parameter is dropped
     * first: under URL session tracking the URI ends with {@code ;jsessionid=...},
     * which is not part of the role name.
     */
    private String roleSegment(HttpServletRequest request) {
        String uri = request.getRequestURI();
        int parameter = uri.indexOf(';');
        if (parameter >= 0) {
            uri = uri.substring(0, parameter);
        }
        return uri.substring(uri.lastIndexOf('/') + 1);
    }
}
