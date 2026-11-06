package com.lumora.sms.dto;

import com.lumora.sms.entity.*;
import java.math.BigDecimal;
import java.util.List;

/**
 * Everything one student screen needs, assembled by the service layer so the
 * controller never reaches past it into repositories.
 */
public record StudentProfile(
        Student student,
        String className,
        String streamName,
        String academicYear,
        List<StudentInvoice> invoices,
        List<ExamResult> results,
        List<BookLoan> loans,
        AttendanceSummaryView attendance,
        BigDecimal outstandingBalance) {

    public String placement() {
        if (className == null) {
            return "Not placed";
        }
        return streamName == null ? className : className + " " + streamName;
    }
}
