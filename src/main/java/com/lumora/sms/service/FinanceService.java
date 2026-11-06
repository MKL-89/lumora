package com.lumora.sms.service;

import com.lumora.sms.dto.*;
import com.lumora.sms.entity.*;
import jakarta.validation.Valid;
import java.util.List;

/** Controls KES fee configuration, invoicing, and balance-safe payments. */
public interface FinanceService {

    FeeCategory saveCategory(FeeCategory category);

    FeeStructure saveStructure(FeeStructure structure);

    StudentInvoice saveInvoice(StudentInvoice invoice);

    Payment recordPayment(@Valid PaymentRequest request);

    List<StudentInvoice> invoices();

    StudentInvoice invoice(Long invoiceId);

    List<StudentInvoice> invoicesFor(Long studentId);

    List<Payment> recentPayments();

    List<FeeStructure> structures();

    List<FeeCategory> categories();

    /**
     * Bills every actively enrolled learner for a term using their class fee
     * structure. Learners already invoiced for that term are skipped, so the run
     * is safe to repeat.
     */
    InvoiceGenerationResult generateInvoicesForTerm(Long termId);

    List<ChartPoint> outstandingByClass();

    List<ChartPoint> collectionsByMethod();
}
