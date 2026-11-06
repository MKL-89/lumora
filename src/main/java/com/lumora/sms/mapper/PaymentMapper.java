package com.lumora.sms.mapper;
import com.lumora.sms.dto.PaymentRequest; import com.lumora.sms.entity.Payment; import org.springframework.stereotype.Component;
/** Maps payment request fields while leaving service-owned relations untouched. */
@Component public class PaymentMapper { public void update(Payment p, PaymentRequest r){ p.setReceiptNumber(r.receiptNumber()); p.setAmount(r.amount()); p.setMethod(r.method()); p.setPaidAt(r.paidAt()); p.setReferenceNumber(r.referenceNumber()); } }
