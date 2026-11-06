package com.lumora.sms.dto;

import java.math.BigDecimal;

/** One line of a subject mark sheet, including any mark already recorded. */
public record MarkSheetRow(
        Long studentId,
        String admissionNumber,
        String fullName,
        BigDecimal marks,
        String grade,
        Integer points) {

    public boolean scored() {
        return marks != null;
    }
}
