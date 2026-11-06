package com.lumora.sms.dto;
import com.lumora.sms.enums.AttendanceStatus; import jakarta.validation.constraints.*; import java.time.LocalDate;
/** One learner's attendance in a stream on a particular day. */
public record AttendanceEntryRequest(@NotNull Long classStreamId, @NotNull Long teacherId, @NotNull Long studentId, @NotNull LocalDate attendanceDate, @NotNull AttendanceStatus status, @Size(max=255) String remarks) { }
