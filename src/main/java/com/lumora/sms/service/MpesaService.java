package com.lumora.sms.service;
import com.lumora.sms.dto.MpesaTransactionRequest; import com.lumora.sms.entity.MpesaTransaction; import jakarta.validation.Valid;
/** Records uniquely referenced M-Pesa confirmations. */
public interface MpesaService { MpesaTransaction record(@Valid MpesaTransactionRequest request); }
