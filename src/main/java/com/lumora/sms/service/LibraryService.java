package com.lumora.sms.service;

import com.lumora.sms.dto.BookLoanRequest;
import com.lumora.sms.entity.*;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;

/** Manages catalogue records and safe physical-copy circulation. */
public interface LibraryService {

    Book saveBook(Book book);

    BookCopy saveCopy(BookCopy copy);

    BookLoan issue(@Valid BookLoanRequest request);

    BookLoan returnBook(Long loanId, LocalDate returnedOn);

    List<Book> books();

    List<BookCopy> availableCopies();

    List<BookLoan> loans();

    List<BookLoan> activeLoans();

    List<BookLoan> overdueLoans();

    List<BookLoan> loansFor(Long borrowerUserId);

    List<BookCategory> categories();
}
