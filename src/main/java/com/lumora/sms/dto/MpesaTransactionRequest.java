package com.lumora.sms.dto;
import jakarta.validation.constraints.*; import java.math.BigDecimal; import java.time.LocalDateTime;
/** M-Pesa confirmation details linked to an already recorded payment. */
public record MpesaTransactionRequest(@NotNull Long paymentId, @NotBlank @Size(max=100) String transactionCode, @NotBlank @Size(max=30) String payerPhone, @NotNull @DecimalMin("0.01") BigDecimal amount, @NotNull LocalDateTime transactionTime, @Size(max=100) String accountReference) { }
