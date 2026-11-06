package com.lumora.sms.repository;

import com.lumora.sms.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    boolean existsByReceiptNumber(String receiptNumber);

    List<Payment> findTop20ByOrderByPaidAtDesc();

    List<Payment> findByStudentInvoiceStudentIdOrderByPaidAtDesc(Long studentId);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p")
    BigDecimal totalCollected();

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.paidAt >= :from")
    BigDecimal collectedSince(@Param("from") LocalDateTime from);

    /** Collections grouped by tender type, for the finance breakdown chart. */
    @Query("SELECT p.method, COALESCE(SUM(p.amount), 0) FROM Payment p GROUP BY p.method")
    List<Object[]> totalsByMethod();
}
