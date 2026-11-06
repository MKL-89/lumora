package com.lumora.sms.service;

import com.lumora.sms.dto.BookLoanRequest;
import com.lumora.sms.entity.*;
import com.lumora.sms.enums.LoanStatus;
import com.lumora.sms.exception.BusinessRuleViolationException;
import com.lumora.sms.repository.*;
import com.lumora.sms.service.impl.LibraryServiceImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Circulation rules. A physical copy exists once, so it must never be on two
 * loans at the same time, and its status has to stay in step with its loan.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("LibraryService circulation")
class LibraryServiceImplTest {

    @Mock private BookRepository books;
    @Mock private BookCopyRepository copies;
    @Mock private BookLoanRepository loans;
    @Mock private BookCategoryRepository categories;
    @Mock private UserRepository users;

    @InjectMocks private LibraryServiceImpl service;

    private BookCopy copy;
    private User borrower;

    @BeforeEach
    void setUp() {
        Book book = new Book();
        book.setId(1L);
        book.setTitle("The River and the Source");
        book.setAuthor("Margaret A. Ogola");

        copy = new BookCopy();
        copy.setId(2L);
        copy.setBook(book);
        copy.setAccessionNumber("LUM/LIB/0001");
        copy.setStatus("AVAILABLE");

        borrower = new User();
        borrower.setId(3L);
        borrower.setUsername("student");

        when(copies.findById(2L)).thenReturn(Optional.of(copy));
        when(users.findById(3L)).thenReturn(Optional.of(borrower));
        when(loans.existsByBookCopyIdAndStatus(anyLong(), any())).thenReturn(false);
        when(loans.save(any(BookLoan.class))).thenAnswer(call -> call.getArgument(0));
    }

    private BookLoanRequest issueRequest(LocalDate borrowedOn, LocalDate dueDate) {
        return new BookLoanRequest(2L, 3L, borrowedOn, dueDate);
    }

    @Test
    @DisplayName("issuing a copy marks it as out")
    void issueMarksTheCopyIssued() {
        BookLoan loan = service.issue(issueRequest(LocalDate.now(), LocalDate.now().plusDays(14)));

        assertThat(loan.getStatus()).isEqualTo(LoanStatus.BORROWED);
        assertThat(loan.getBorrower()).isEqualTo(borrower);
        assertThat(copy.getStatus()).isEqualTo("ISSUED");
    }

    @Test
    @DisplayName("a copy already out cannot be issued again")
    void issuedCopyCannotBeReissued() {
        copy.setStatus("ISSUED");

        assertThatThrownBy(() -> service.issue(issueRequest(LocalDate.now(), LocalDate.now().plusDays(14))))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("not available to issue");

        verify(loans, never()).save(any());
    }

    @Test
    @DisplayName("a copy with an open loan cannot be issued, whatever its status field says")
    void openLoanBlocksIssue() {
        when(loans.existsByBookCopyIdAndStatus(2L, LoanStatus.BORROWED)).thenReturn(true);

        assertThatThrownBy(() -> service.issue(issueRequest(LocalDate.now(), LocalDate.now().plusDays(14))))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    @DisplayName("a loan cannot fall due before it is issued")
    void dueDateMustFollowIssueDate() {
        assertThatThrownBy(() -> service.issue(issueRequest(LocalDate.now(), LocalDate.now().minusDays(1))))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("due before it is issued");
    }

    @Test
    @DisplayName("returning a copy puts it back on the shelf")
    void returnRestoresAvailability() {
        BookLoan loan = service.issue(issueRequest(LocalDate.now().minusDays(3), LocalDate.now().plusDays(11)));
        loan.setId(9L);
        when(loans.findById(9L)).thenReturn(Optional.of(loan));

        BookLoan returned = service.returnBook(9L, LocalDate.now());

        assertThat(returned.getStatus()).isEqualTo(LoanStatus.RETURNED);
        assertThat(returned.getReturnedOn()).isEqualTo(LocalDate.now());
        assertThat(copy.getStatus()).isEqualTo("AVAILABLE");
    }

    @Test
    @DisplayName("a copy cannot be returned twice")
    void returnedLoanCannotBeReturnedAgain() {
        BookLoan loan = new BookLoan();
        loan.setId(9L);
        loan.setBookCopy(copy);
        loan.setStatus(LoanStatus.RETURNED);
        when(loans.findById(9L)).thenReturn(Optional.of(loan));

        assertThatThrownBy(() -> service.returnBook(9L, LocalDate.now()))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("Only an active loan");
    }

    @Test
    @DisplayName("a copy cannot be returned before it was issued")
    void returnCannotPredateIssue() {
        BookLoan loan = new BookLoan();
        loan.setId(9L);
        loan.setBookCopy(copy);
        loan.setStatus(LoanStatus.BORROWED);
        loan.setBorrowedOn(LocalDate.now());
        when(loans.findById(9L)).thenReturn(Optional.of(loan));

        assertThatThrownBy(() -> service.returnBook(9L, LocalDate.now().minusDays(2)))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("before it was issued");
    }
}
