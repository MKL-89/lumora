package com.lumora.sms.service.impl;

import com.lumora.sms.dto.*;
import com.lumora.sms.entity.*;
import com.lumora.sms.enums.InvoiceStatus;
import com.lumora.sms.exception.*;
import com.lumora.sms.mapper.PaymentMapper;
import com.lumora.sms.repository.*;
import com.lumora.sms.service.FinanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;

/** Implements KES invoicing and the payment balance rules. */
@Service
@org.springframework.validation.annotation.Validated
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FinanceServiceImpl implements FinanceService {

    private final FeeCategoryRepository categories;
    private final FeeStructureRepository structures;
    private final FeeStructureItemRepository structureItems;
    private final StudentInvoiceRepository invoices;
    private final PaymentRepository payments;
    private final UserRepository users;
    private final TermRepository terms;
    private final StudentEnrollmentRepository enrollments;
    private final PaymentMapper mapper;

    @Override
    @Transactional
    public FeeCategory saveCategory(FeeCategory category) {
        return categories.save(category);
    }

    @Override
    @Transactional
    public FeeStructure saveStructure(FeeStructure structure) {
        return structures.save(structure);
    }

    @Override
    @Transactional
    public StudentInvoice saveInvoice(StudentInvoice invoice) {
        if (invoice.getBalanceAmount().compareTo(BigDecimal.ZERO) > 0 && invoice.getStatus() == InvoiceStatus.PAID) {
            throw new BusinessRuleViolationException("An invoice with a balance cannot be marked fully paid.");
        }
        return invoices.save(invoice);
    }

    @Override
    @Transactional
    public Payment recordPayment(@Valid PaymentRequest r) {
        if (payments.existsByReceiptNumber(r.receiptNumber())) {
            throw new DuplicateResourceException("Receipt number", r.receiptNumber());
        }
        StudentInvoice invoice = invoices.findById(r.invoiceId())
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", r.invoiceId()));

        if (invoice.getStatus() == InvoiceStatus.VOID) {
            throw new BusinessRuleViolationException("A voided invoice cannot receive payments.");
        }
        if (r.amount().compareTo(invoice.getBalanceAmount()) > 0) {
            throw new BusinessRuleViolationException("A payment of KES " + r.amount()
                    + " exceeds the outstanding balance of KES " + invoice.getBalanceAmount() + ".");
        }

        Payment payment = new Payment();
        mapper.update(payment, r);
        payment.setStudentInvoice(invoice);
        if (r.receivedByUserId() != null) {
            payment.setReceivedBy(users.findById(r.receivedByUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", r.receivedByUserId())));
        }

        invoice.setBalanceAmount(invoice.getBalanceAmount().subtract(r.amount()));
        invoice.setStatus(invoice.getBalanceAmount().signum() == 0
                ? InvoiceStatus.PAID
                : InvoiceStatus.PARTIALLY_PAID);
        return payments.save(payment);
    }

    @Override
    public List<StudentInvoice> invoices() {
        return invoices.findAllByOrderByDueDateDesc();
    }

    @Override
    public StudentInvoice invoice(Long invoiceId) {
        return invoices.findById(invoiceId).orElseThrow(() -> new ResourceNotFoundException("Invoice", invoiceId));
    }

    @Override
    public List<StudentInvoice> invoicesFor(Long studentId) {
        return invoices.findByStudentIdOrderByDueDateDesc(studentId);
    }

    @Override
    public List<Payment> recentPayments() {
        return payments.findTop20ByOrderByPaidAtDesc();
    }

    @Override
    public List<FeeStructure> structures() {
        return structures.findAll();
    }

    @Override
    public List<FeeCategory> categories() {
        return categories.findByActiveTrueOrderByNameAsc();
    }

    /**
     * Bills every actively enrolled learner for the term. The amount comes from the
     * fee structure attached to the learner's class; learners whose class has no
     * structure, and those already invoiced for the term, are skipped so the run is
     * repeatable without creating duplicates.
     */
    @Override
    @Transactional
    public InvoiceGenerationResult generateInvoicesForTerm(Long termId) {
        Term term = terms.findById(termId).orElseThrow(() -> new ResourceNotFoundException("Term", termId));

        int created = 0;
        int skipped = 0;
        BigDecimal billed = BigDecimal.ZERO;

        for (StudentEnrollment enrollment : enrollments.findAll()) {
            if (!enrollment.isActive() || !enrollment.getStudent().isActive()) {
                continue;
            }
            Student student = enrollment.getStudent();
            SchoolClass schoolClass = enrollment.getClassStream().getSchoolClass();

            if (invoices.existsByStudentIdAndTermId(student.getId(), termId)) {
                skipped++;
                continue;
            }
            FeeStructure structure = structures.findByTermIdAndSchoolClassId(termId, schoolClass.getId()).orElse(null);
            if (structure == null) {
                skipped++;
                continue;
            }
            BigDecimal amount = structureItems.totalFor(structure.getId());
            if (amount.signum() <= 0) {
                skipped++;
                continue;
            }

            StudentInvoice invoice = new StudentInvoice();
            invoice.setStudent(student);
            invoice.setTerm(term);
            invoice.setInvoiceNumber(nextInvoiceNumber(term, student));
            invoice.setTotalAmount(amount);
            invoice.setBalanceAmount(amount);
            invoice.setStatus(InvoiceStatus.ISSUED);
            invoice.setDueDate(term.getStartDate().plusDays(30));
            invoices.save(invoice);

            created++;
            billed = billed.add(amount);
        }
        return new InvoiceGenerationResult(created, skipped, billed);
    }

    /** Human-readable and unique: INV-{year}T{term}-{admission number}. */
    private String nextInvoiceNumber(Term term, Student student) {
        StringBuilder year = new StringBuilder();
        for (char c : term.getAcademicYear().getName().toCharArray()) {
            if (Character.isDigit(c)) {
                year.append(c);
            }
        }
        return "INV-" + year + "T" + term.getTermNumber() + "-" + student.getAdmissionNumber();
    }

    @Override
    public List<ChartPoint> outstandingByClass() {
        return invoices.outstandingByClass().stream()
                .map(row -> ChartPoint.of((String) row[0], (Number) row[1]))
                .toList();
    }

    @Override
    public List<ChartPoint> collectionsByMethod() {
        return payments.totalsByMethod().stream()
                .map(row -> ChartPoint.of(String.valueOf(row[0]), (Number) row[1]))
                .toList();
    }
}
