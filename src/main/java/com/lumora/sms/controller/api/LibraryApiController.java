package com.lumora.sms.controller.api;

import com.lumora.sms.dto.BookLoanRequest;
import com.lumora.sms.entity.BookLoan;
import com.lumora.sms.service.LibraryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;

/** Protected REST circulation endpoints connected directly to LibraryService. */
@RestController
@RequestMapping("/api/library")
@RequiredArgsConstructor
public class LibraryApiController {

    private final LibraryService library;

    @PostMapping("/loans")
    public Long issue(@Valid @RequestBody BookLoanRequest request) {
        return library.issue(request).getId();
    }

    @PostMapping("/loans/{loanId}/return")
    public String returnBook(@PathVariable Long loanId) {
        BookLoan loan = library.returnBook(loanId, LocalDate.now());
        return loan.getBookCopy().getAccessionNumber();
    }
}
