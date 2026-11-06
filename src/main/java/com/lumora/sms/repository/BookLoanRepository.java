package com.lumora.sms.repository;

import com.lumora.sms.entity.BookLoan;
import com.lumora.sms.enums.LoanStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;

public interface BookLoanRepository extends JpaRepository<BookLoan, Long> {

    boolean existsByBookCopyIdAndStatus(Long bookCopyId, LoanStatus status);

    List<BookLoan> findByStatusOrderByDueDateAsc(LoanStatus status);

    List<BookLoan> findByBorrowerIdOrderByBorrowedOnDesc(Long borrowerId);

    List<BookLoan> findAllByOrderByBorrowedOnDesc();

    long countByStatus(LoanStatus status);

    /** Active loans already past their due date. */
    @Query("""
           SELECT l FROM BookLoan l
           WHERE l.status = com.lumora.sms.enums.LoanStatus.BORROWED AND l.dueDate < :today
           ORDER BY l.dueDate
           """)
    List<BookLoan> findOverdue(@Param("today") LocalDate today);
}
