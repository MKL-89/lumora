package com.lumora.sms.controller.api;
import com.lumora.sms.dto.AttendanceEntryRequest; import com.lumora.sms.service.AttendanceService; import jakarta.validation.Valid; import lombok.RequiredArgsConstructor; import org.springframework.web.bind.annotation.*;
/** Protected REST write endpoint connected directly to AttendanceService. */
@RestController @RequestMapping("/api/attendance") @RequiredArgsConstructor public class AttendanceApiController { private final AttendanceService attendance; @PostMapping public Object record(@Valid @RequestBody AttendanceEntryRequest request){return attendance.record(request);} }
