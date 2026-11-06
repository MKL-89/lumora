package com.lumora.sms.dto;

import java.math.BigDecimal;

/**
 * Whole-school headline figures shown on the dashboard stat cards.
 *
 * @param attendanceRate percentage of learners marked present today, or -1 when
 *                       no register has been taken yet.
 */
public record DashboardSummary(
        long students,
        long teachers,
        long invoices,
        long activeLoans,
        long overdueLoans,
        long unpublishedExaminations,
        int attendanceRate,
        BigDecimal totalBilled,
        BigDecimal totalCollected,
        BigDecimal totalOutstanding) {

    /** Percentage of billed fees actually collected, rounded to a whole number. */
    public int collectionRate() {
        if (totalBilled == null || totalBilled.signum() == 0) {
            return 0;
        }
        return totalCollected.multiply(BigDecimal.valueOf(100))
                .divide(totalBilled, 0, java.math.RoundingMode.HALF_UP)
                .intValue();
    }
}
