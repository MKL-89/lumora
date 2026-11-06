package com.lumora.sms.controller.api;
import com.lumora.sms.dto.MarkEntryRequest; import com.lumora.sms.service.ExaminationService; import jakarta.validation.Valid; import lombok.RequiredArgsConstructor; import org.springframework.web.bind.annotation.*;
/** Protected REST mark-entry endpoint; teacher authorization is enforced by the service. */
@RestController @RequestMapping("/api/results") @RequiredArgsConstructor public class ExaminationApiController { private final ExaminationService examinations; @PostMapping public Object enter(@Valid @RequestBody MarkEntryRequest request){return examinations.enterMark(request);} }
