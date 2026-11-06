package com.lumora.sms.controller.api;

import com.lumora.sms.dto.MarkEntryRequest;
import com.lumora.sms.security.ActingTeacher;
import com.lumora.sms.service.ExaminationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/** Mark entry over REST. A signed-in teacher is always recorded as themselves. */
@RestController
@RequestMapping("/api/results")
@RequiredArgsConstructor
public class ExaminationApiController {

    private final ExaminationService examinations;
    private final ActingTeacher actingTeacher;

    @PostMapping
    public Object enter(@Valid @RequestBody MarkEntryRequest request, Authentication authentication) {
        Long teacherId = actingTeacher.resolve(authentication, request.teacherId());
        return examinations.enterMark(new MarkEntryRequest(
                teacherId, request.examSubjectId(), request.studentId(), request.marks(), request.remarks()));
    }
}
