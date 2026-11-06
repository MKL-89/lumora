package com.lumora.sms.repository;

import com.lumora.sms.entity.StudentInvoice;
import com.lumora.sms.enums.InvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface StudentInvoiceRepository extends JpaRepository<StudentInvoice, Long> {

    Optional<StudentInvoice> findByInvoiceNumber(String invoiceNumber);

    boolean existsByInvoiceNumber(String invoiceNumber);

    boolean existsByStudentIdAndTermId(Long studentId, Long termId);

    List<StudentInvoice> findByStudentIdOrderByDueDateDesc(Long studentId);

    List<StudentInvoice> findAllByOrderByDueDateDesc();

    long countByStatus(InvoiceStatus status);

    @Query("SELECT COALESCE(SUM(i.totalAmount), 0) FROM StudentInvoice i")
    BigDecimal totalBilled();

    @Query("SELECT COALESCE(SUM(i.balanceAmount), 0) FROM StudentInvoice i")
    BigDecimal totalOutstanding();

    /** Outstanding balance per class, for the fee-collection chart. */
    @Query("""
           SELECT e.classStream.schoolClass.name, COALESCE(SUM(i.balanceAmount), 0)
           FROM StudentInvoice i
           JOIN StudentEnrollment e ON e.student.id = i.student.id AND e.active = true
           GROUP BY e.classStream.schoolClass.name
           ORDER BY e.classStream.schoolClass.name
           """)
    List<Object[]> outstandingByClass();
}
