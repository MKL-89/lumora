package com.lumora.sms.dto;

import java.math.BigDecimal;

/** Outcome of a bulk invoice run, reported back to the bursar. */
public record InvoiceGenerationResult(int created, int skipped, BigDecimal billed) { }
