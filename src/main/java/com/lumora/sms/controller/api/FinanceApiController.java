package com.lumora.sms.controller.api;
import com.lumora.sms.dto.*; import com.lumora.sms.service.*; import jakarta.validation.Valid; import lombok.RequiredArgsConstructor; import org.springframework.web.bind.annotation.*;
/** Protected REST endpoints for payment and M-Pesa recording. */
@RestController @RequestMapping("/api/finance") @RequiredArgsConstructor public class FinanceApiController { private final FinanceService finance; private final MpesaService mpesa; @PostMapping("/payments") public Object payment(@Valid @RequestBody PaymentRequest request){return finance.recordPayment(request);}@PostMapping("/mpesa") public Object mpesa(@Valid @RequestBody MpesaTransactionRequest request){return mpesa.record(request);} }
