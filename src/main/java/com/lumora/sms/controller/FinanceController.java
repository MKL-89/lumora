package com.lumora.sms.controller;

import com.lumora.sms.dto.PaymentRequest;
import com.lumora.sms.enums.PaymentMethod;
import com.lumora.sms.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Fee structures, student invoices, and receipting. */
@Controller
@RequiredArgsConstructor
public class FinanceController {

    private final FinanceService finance;
    private final AcademicService academics;

    @GetMapping("/fees")
    public String fees(Model model) {
        model.addAttribute("module", "Fees");
        model.addAttribute("invoices", finance.invoices());
        model.addAttribute("structures", finance.structures());
        model.addAttribute("categories", finance.categories());
        model.addAttribute("terms", academics.terms());
        model.addAttribute("activeTerm", academics.activeTerm());
        model.addAttribute("outstandingChart", finance.outstandingByClass());
        return "modules/fees";
    }

    /** Bills every actively enrolled learner for the chosen term. Safe to repeat. */
    @PostMapping("/fees/generate")
    public String generate(@RequestParam Long termId, RedirectAttributes flash) {
        var result = finance.generateInvoicesForTerm(termId);
        flash.addFlashAttribute("message", "Invoice run complete: " + result.created()
                + " created, " + result.skipped() + " skipped, KES " + result.billed() + " billed.");
        return "redirect:/fees";
    }

    @GetMapping("/payments")
    public String payments(Model model) {
        model.addAttribute("module", "Payments");
        model.addAttribute("payments", finance.recentPayments());
        model.addAttribute("invoices", finance.invoices());
        model.addAttribute("methods", PaymentMethod.values());
        model.addAttribute("collectionChart", finance.collectionsByMethod());
        return "modules/payments";
    }

    @PostMapping("/payments")
    public String recordPayment(@RequestParam Long invoiceId,
                                @RequestParam String receiptNumber,
                                @RequestParam BigDecimal amount,
                                @RequestParam PaymentMethod method,
                                @RequestParam(required = false) String referenceNumber,
                                RedirectAttributes flash) {
        var payment = finance.recordPayment(new PaymentRequest(
                invoiceId, receiptNumber.trim(), amount, method, LocalDateTime.now(),
                referenceNumber == null || referenceNumber.isBlank() ? null : referenceNumber.trim(),
                null));

        flash.addFlashAttribute("message", "Receipt " + payment.getReceiptNumber()
                + " recorded. Invoice balance is now KES " + payment.getStudentInvoice().getBalanceAmount() + ".");
        return "redirect:/payments";
    }
}
