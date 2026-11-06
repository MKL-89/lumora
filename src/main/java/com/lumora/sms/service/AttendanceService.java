package com.lumora.sms.service;

import com.lumora.sms.dto.*;
import com.lumora.sms.entity.AttendanceRecord;
import com.lumora.sms.enums.AttendanceStatus;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Records a learner once per class stream and school day. */
public interface AttendanceService {

    AttendanceRecord record(@Valid AttendanceEntryRequest request);

    /** The register for a stream on a date, pre-filled with anything already saved. */
    List<RegisterRow> register(Long classStreamId, LocalDate date);

    /**
     * Saves a whole register in one transaction. Existing rows are updated rather
     * than rejected, so a teacher can correct a mistake without an admin.
     *
     * @return the number of learners written
     */
    int saveRegister(Long classStreamId, Long teacherId, LocalDate date, Map<Long, AttendanceStatus> marks);

    AttendanceSummaryView summaryFor(Long studentId);

    List<ChartPoint> attendanceTrend(int days);
}
