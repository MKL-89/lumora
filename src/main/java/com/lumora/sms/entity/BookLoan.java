package com.lumora.sms.entity;
import com.lumora.sms.enums.LoanStatus; import jakarta.persistence.*; import lombok.*; import java.time.LocalDate;
@Entity @Table(name="book_loans") @Getter @Setter @NoArgsConstructor
public class BookLoan extends BaseEntity { @ManyToOne(optional=false) @JoinColumn(name="book_copy_id") private BookCopy bookCopy; @ManyToOne(optional=false) @JoinColumn(name="user_id") private User borrower; @Column(nullable=false) private LocalDate borrowedOn; @Column(nullable=false) private LocalDate dueDate; private LocalDate returnedOn; @Enumerated(EnumType.STRING) @Column(nullable=false,length=15) private LoanStatus status; }
