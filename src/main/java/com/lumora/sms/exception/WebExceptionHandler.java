package com.lumora.sms.exception;

import com.lumora.sms.security.audit.SecurityAuditService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/**
 * Renders failures raised by page controllers as a styled error screen instead of
 * a stack trace, keeping the message safe to show a school user.
 */
@Slf4j
@Order(Ordered.LOWEST_PRECEDENCE)
@ControllerAdvice(basePackages = "com.lumora.sms.controller")
@RequiredArgsConstructor
public class WebExceptionHandler {

    private final SecurityAuditService audit;

    @ExceptionHandler({BusinessRuleViolationException.class, DuplicateResourceException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String business(RuntimeException ex, HttpServletRequest request, Model model) {
        return render(model, request, 400, "That action was not allowed", ex.getMessage());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String missing(ResourceNotFoundException ex, HttpServletRequest request, Model model) {
        return render(model, request, 404, "Record not found", ex.getMessage());
    }

    /**
     * Record-level refusals are audited here. Route-level refusals never reach a
     * controller, so those are audited by LumoraAccessDeniedHandler instead.
     */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String denied(AccessDeniedException ex, HttpServletRequest request, Model model) {
        if (request.getUserPrincipal() != null) {
            audit.record(request.getUserPrincipal().getName(), "ACCESS_DENIED", request.getRemoteAddr());
        }
        return render(model, request, 403, "Access denied", ex.getMessage());
    }

    /** Anything unexpected is logged in full but reported to the user in general terms. */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String unexpected(Exception ex, HttpServletRequest request, Model model) {
        log.error("Unhandled error at {}", request.getRequestURI(), ex);
        return render(model, request, 500, "Something went wrong",
                "The system could not complete that request. The details have been logged.");
    }

    private String render(Model model, HttpServletRequest request, int status, String title, String detail) {
        model.addAttribute("status", status);
        model.addAttribute("title", title);
        model.addAttribute("detail", detail);
        model.addAttribute("path", cleanPath(request));
        return "error";
    }

    /** Never echo a session id back onto the page. */
    private String cleanPath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        int parameter = uri.indexOf(';');
        return parameter >= 0 ? uri.substring(0, parameter) : uri;
    }
}
