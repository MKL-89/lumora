package com.lumora.sms.dto;
import jakarta.validation.constraints.*; import java.math.BigDecimal;
/** A teacher's mark entry for an enrolled learner. */
public record MarkEntryRequest(@NotNull Long teacherId, @NotNull Long examSubjectId, @NotNull Long studentId, @NotNull @DecimalMin("0.00") BigDecimal marks, @Size(max=255) String remarks) { }
