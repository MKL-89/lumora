package com.lumora.sms.dto;

import com.lumora.sms.enums.AttendanceStatus;

/**
 * One line of the daily attendance register.
 *
 * @param status the status already on file, or {@code null} when the learner has
 *               not been marked for this session yet.
 */
public record RegisterRow(Long studentId, String admissionNumber, String fullName, AttendanceStatus status) {

    public boolean alreadyMarked() {
        return status != null;
    }
}
