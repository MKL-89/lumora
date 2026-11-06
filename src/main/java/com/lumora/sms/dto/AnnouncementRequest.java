package com.lumora.sms.dto;

import jakarta.validation.constraints.*;

/** Input for publishing a school-wide notice. */
public record AnnouncementRequest(
        @NotBlank @Size(max = 150) String title,
        @NotBlank @Size(max = 5000) String content,
        Integer daysVisible) { }
