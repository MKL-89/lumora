package com.lumora.sms.controller;

import com.lumora.sms.dto.BookLoanRequest;
import com.lumora.sms.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.time.LocalDate;

/** The catalogue and the circulation desk. */
@Controller
@RequiredArgsConstructor
public class LibraryController {

    private final LibraryService library;
    private final UserService users;

    @GetMapping("/library")
    public String circulation(Model model) {
        model.addAttribute("module", "Library circulation");
        model.addAttribute("loans", library.loans());
        model.addAttribute("overdue", library.overdueLoans());
        model.addAttribute("availableCopies", library.availableCopies());
        model.addAttribute("borrowers", users.borrowers());
        model.addAttribute("today", LocalDate.now());
        model.addAttribute("defaultDue", LocalDate.now().plusDays(14));
        return "modules/library";
    }

    @GetMapping("/books")
    public String catalogue(Model model) {
        model.addAttribute("module", "Book catalogue");
        model.addAttribute("books", library.books());
        model.addAttribute("categories", library.categories());
        return "modules/books";
    }

    @PostMapping("/library/issue")
    public String issue(@RequestParam Long bookCopyId,
                        @RequestParam Long borrowerUserId,
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueDate,
                        RedirectAttributes flash) {
        var loan = library.issue(new BookLoanRequest(bookCopyId, borrowerUserId, LocalDate.now(), dueDate));
        flash.addFlashAttribute("message", "Issued " + loan.getBookCopy().getBook().getTitle()
                + " (copy " + loan.getBookCopy().getAccessionNumber() + ") to "
                + loan.getBorrower().getUsername() + ", due " + loan.getDueDate() + ".");
        return "redirect:/library";
    }

    @PostMapping("/library/return/{loanId}")
    public String returnBook(@PathVariable Long loanId, RedirectAttributes flash) {
        var loan = library.returnBook(loanId, LocalDate.now());
        flash.addFlashAttribute("message", "Copy " + loan.getBookCopy().getAccessionNumber()
                + " returned and back on the shelf.");
        return "redirect:/library";
    }
}
