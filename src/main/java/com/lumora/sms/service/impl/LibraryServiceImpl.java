package com.lumora.sms.service.impl;

import com.lumora.sms.dto.BookLoanRequest;
import com.lumora.sms.entity.*;
import com.lumora.sms.enums.LoanStatus;
import com.lumora.sms.exception.*;
import com.lumora.sms.repository.*;
import com.lumora.sms.service.LibraryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;

/** Coordinates book-copy status with issue and return transactions. */
@Service
@org.springframework.validation.annotation.Validated
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LibraryServiceImpl implements LibraryService {

    static final String AVAILABLE = "AVAILABLE";
    static final String ISSUED = "ISSUED";

    private final BookRepository books;
    private final BookCopyRepository copies;
    private final BookLoanRepository loans;
    private final BookCategoryRepository categories;
    private final UserRepository users;

    @Override
    @Transactional
    public Book saveBook(Book book) {
        return books.save(book);
    }

    @Override
    @Transactional
    public BookCopy saveCopy(BookCopy copy) {
        return copies.save(copy);
    }

    @Override
    @Transactional
    public BookLoan issue(@Valid BookLoanRequest r) {
        BookCopy copy = copies.findById(r.bookCopyId())
                .orElseThrow(() -> new ResourceNotFoundException("Book copy", r.bookCopyId()));

        if (!AVAILABLE.equals(copy.getStatus())
                || loans.existsByBookCopyIdAndStatus(copy.getId(), LoanStatus.BORROWED)) {
            throw new BusinessRuleViolationException(
                    "Copy " + copy.getAccessionNumber() + " is not available to issue.");
        }
        if (r.dueDate().isBefore(r.borrowedOn())) {
            throw new BusinessRuleViolationException("A loan cannot fall due before it is issued.");
        }

        BookLoan loan = new BookLoan();
        loan.setBookCopy(copy);
        loan.setBorrower(users.findById(r.borrowerUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", r.borrowerUserId())));
        loan.setBorrowedOn(r.borrowedOn());
        loan.setDueDate(r.dueDate());
        loan.setStatus(LoanStatus.BORROWED);
        copy.setStatus(ISSUED);
        return loans.save(loan);
    }

    @Override
    @Transactional
    public BookLoan returnBook(Long id, LocalDate date) {
        BookLoan loan = loans.findById(id).orElseThrow(() -> new ResourceNotFoundException("Book loan", id));

        if (loan.getStatus() != LoanStatus.BORROWED && loan.getStatus() != LoanStatus.OVERDUE) {
            throw new BusinessRuleViolationException("Only an active loan can be returned.");
        }
        LocalDate returnedOn = date == null ? LocalDate.now() : date;
        if (returnedOn.isBefore(loan.getBorrowedOn())) {
            throw new BusinessRuleViolationException("A book cannot be returned before it was issued.");
        }

        loan.setReturnedOn(returnedOn);
        loan.setStatus(LoanStatus.RETURNED);
        loan.getBookCopy().setStatus(AVAILABLE);
        return loans.save(loan);
    }

    @Override
    public List<Book> books() {
        return books.findAllByOrderByTitleAsc();
    }

    @Override
    public List<BookCopy> availableCopies() {
        return copies.findByStatusOrderByAccessionNumberAsc(AVAILABLE);
    }

    @Override
    public List<BookLoan> loans() {
        return loans.findAllByOrderByBorrowedOnDesc();
    }

    @Override
    public List<BookLoan> activeLoans() {
        return loans.findByStatusOrderByDueDateAsc(LoanStatus.BORROWED);
    }

    @Override
    public List<BookLoan> overdueLoans() {
        return loans.findOverdue(LocalDate.now());
    }

    @Override
    public List<BookLoan> loansFor(Long borrowerUserId) {
        return loans.findByBorrowerIdOrderByBorrowedOnDesc(borrowerUserId);
    }

    @Override
    public List<BookCategory> categories() {
        return categories.findAllByOrderByNameAsc();
    }
}
