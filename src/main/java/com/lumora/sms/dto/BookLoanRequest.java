package com.lumora.sms.dto;
import jakarta.validation.constraints.*; import java.time.LocalDate;
/** Request to issue a physical library copy. */
public record BookLoanRequest(@NotNull Long bookCopyId, @NotNull Long borrowerUserId, @NotNull LocalDate borrowedOn, @NotNull @FutureOrPresent LocalDate dueDate) { }
