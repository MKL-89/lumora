package com.lumora.sms.service;

import com.lumora.sms.dto.PaymentRequest;
import com.lumora.sms.entity.*;
import com.lumora.sms.enums.InvoiceStatus;
import com.lumora.sms.enums.PaymentMethod;
import com.lumora.sms.exception.*;
import com.lumora.sms.mapper.PaymentMapper;
import com.lumora.sms.repository.*;
import com.lumora.sms.service.impl.FinanceServiceImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Money rules. An invoice must never go negative, a receipt number must never be
 * reused, and an invoice is only PAID at a zero balance.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("FinanceService payments")
class FinanceServiceImplTest {

    @Mock private FeeCategoryRepository categories;
    @Mock private FeeStructureRepository structures;
    @Mock private FeeStructureItemRepository structureItems;
    @Mock private StudentInvoiceRepository invoices;
    @Mock private PaymentRepository payments;
    @Mock private UserRepository users;
    @Mock private TermRepository terms;
    @Mock private StudentEnrollmentRepository enrollments;
    @Spy  private PaymentMapper mapper = new PaymentMapper();

    @InjectMocks private FinanceServiceImpl service;

    private StudentInvoice invoice;

    @BeforeEach
    void setUp() {
        invoice = new StudentInvoice();
        invoice.setId(1L);
        invoice.setInvoiceNumber("INV-2026T2-ADM/2026/001");
        invoice.setTotalAmount(new BigDecimal("37000.00"));
        invoice.setBalanceAmount(new BigDecimal("37000.00"));
        invoice.setStatus(InvoiceStatus.ISSUED);

        when(invoices.findById(1L)).thenReturn(Optional.of(invoice));
        when(payments.existsByReceiptNumber(anyString())).thenReturn(false);
        when(payments.save(any(Payment.class))).thenAnswer(call -> call.getArgument(0));
    }

    private PaymentRequest pay(String receipt, String amount) {
        return new PaymentRequest(1L, receipt, new BigDecimal(amount), PaymentMethod.MPESA,
                LocalDateTime.now(), "SGL123456", null);
    }

    @Test
    @DisplayName("a part payment reduces the balance and marks the invoice partially paid")
    void partPayment() {
        service.recordPayment(pay("RCP-1", "10000.00"));

        assertThat(invoice.getBalanceAmount()).isEqualByComparingTo("27000.00");
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.PARTIALLY_PAID);
    }

    @Test
    @DisplayName("clearing the balance exactly marks the invoice paid")
    void fullPayment() {
        service.recordPayment(pay("RCP-2", "37000.00"));

        assertThat(invoice.getBalanceAmount()).isEqualByComparingTo("0");
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.PAID);
    }

    @Test
    @DisplayName("successive payments accumulate correctly")
    void successivePayments() {
        service.recordPayment(pay("RCP-3", "20000.00"));
        service.recordPayment(pay("RCP-4", "17000.00"));

        assertThat(invoice.getBalanceAmount()).isEqualByComparingTo("0");
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.PAID);
    }

    @Test
    @DisplayName("a payment larger than the balance is refused")
    void overpaymentIsRefused() {
        assertThatThrownBy(() -> service.recordPayment(pay("RCP-5", "40000.00")))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("exceeds the outstanding balance");

        assertThat(invoice.getBalanceAmount()).isEqualByComparingTo("37000.00");
        verify(payments, never()).save(any());
    }

    @Test
    @DisplayName("a receipt number can only be used once")
    void duplicateReceiptIsRefused() {
        when(payments.existsByReceiptNumber("RCP-DUP")).thenReturn(true);

        assertThatThrownBy(() -> service.recordPayment(pay("RCP-DUP", "100.00")))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Receipt number");

        verify(payments, never()).save(any());
    }

    @Test
    @DisplayName("a voided invoice cannot receive payments")
    void voidedInvoiceIsRefused() {
        invoice.setStatus(InvoiceStatus.VOID);

        assertThatThrownBy(() -> service.recordPayment(pay("RCP-6", "100.00")))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("voided");
    }

    @Test
    @DisplayName("an invoice with a balance cannot be saved as fully paid")
    void inconsistentInvoiceIsRefused() {
        StudentInvoice bogus = new StudentInvoice();
        bogus.setBalanceAmount(new BigDecimal("500.00"));
        bogus.setStatus(InvoiceStatus.PAID);

        assertThatThrownBy(() -> service.saveInvoice(bogus))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("cannot be marked fully paid");
    }
}
