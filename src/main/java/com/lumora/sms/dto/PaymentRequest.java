package com.lumora.sms.dto;
import com.lumora.sms.enums.PaymentMethod; import jakarta.validation.constraints.*; import java.math.BigDecimal; import java.time.LocalDateTime;
/** Payment input expressed in Kenyan shillings (KES). */
public record PaymentRequest(@NotNull Long invoiceId, @NotBlank @Size(max=40) String receiptNumber, @NotNull @DecimalMin("0.01") BigDecimal amount, @NotNull PaymentMethod method, @NotNull LocalDateTime paidAt, @Size(max=100) String referenceNumber, Long receivedByUserId) { }
