package com.lumora.sms.dto;

import jakarta.validation.constraints.*;
import java.util.List;

/**
 * Everything first-time setup needs to turn an empty database into a working
 * school with one administrator who can sign in.
 */
public record SetupRequest(
        @NotBlank(message = "Enter the school's name.") @Size(max = 150) String schoolName,
        @Size(max = 30) String schoolCode,
        @Size(max = 80) String county,
        @Size(max = 30) String phone,
        @Email(message = "The school email address is not valid.") @Size(max = 150) String schoolEmail,
        @NotNull @Min(2020) @Max(2100) Integer year,
        @NotNull @Min(1) @Max(3) Integer currentTerm,
        @NotBlank(message = "Give at least one stream name.") @Size(max = 200) String streams,
        List<String> subjects,
        @NotBlank(message = "Choose a username for the administrator.") @Size(max = 80)
        @Pattern(regexp = "[A-Za-z0-9._-]*",
                message = "The username may use letters, numbers, dots, dashes and underscores only.")
        String adminUsername,
        @NotBlank(message = "Enter the administrator's email address.")
        @Email(message = "The administrator's email address is not valid.") @Size(max = 150) String adminEmail,
        @NotBlank(message = "Choose a password.") String adminPassword,
        @NotBlank(message = "Type the password a second time.") String adminPasswordConfirm) {
}
