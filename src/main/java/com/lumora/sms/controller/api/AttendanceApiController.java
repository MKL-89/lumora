package com.lumora.sms.controller.api;

import com.lumora.sms.dto.AttendanceEntryRequest;
import com.lumora.sms.security.ActingTeacher;
import com.lumora.sms.service.AttendanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/** Attendance over REST. A signed-in teacher is always recorded as themselves. */
@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class AttendanceApiController {

    private final AttendanceService attendance;
    private final ActingTeacher actingTeacher;

    @PostMapping
    public Object record(@Valid @RequestBody AttendanceEntryRequest request, Authentication authentication) {
        Long teacherId = actingTeacher.resolve(authentication, request.teacherId());
        return attendance.record(new AttendanceEntryRequest(request.classStreamId(), teacherId,
                request.studentId(), request.attendanceDate(), request.status(), request.remarks()));
    }
}
