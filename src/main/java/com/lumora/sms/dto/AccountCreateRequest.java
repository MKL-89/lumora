package com.lumora.sms.dto;

import com.lumora.sms.enums.UserRoleType;
import jakarta.validation.constraints.*;
import java.util.Set;

/**
 * An account an administrator creates for a member of staff, a learner or a
 * guardian. No password is typed here: the system generates a temporary one that
 * the holder must replace the first time they sign in.
 *
 * <p>Teaching roles also need an employee number, because that creates the
 * teacher record marks and registers are recorded against. Learner and guardian
 * accounts are linked to a learner by admission number.
 */
public record AccountCreateRequest(
        @NotBlank(message = "Choose a username.") @Size(max = 80)
        @Pattern(regexp = "[A-Za-z0-9._-]*",
                message = "The username may use letters, numbers, dots, dashes and underscores only.")
        String username,
        @NotBlank(message = "Enter an email address.") @Email(message = "The email address is not valid.")
        @Size(max = 150) String email,
        @NotBlank(message = "Enter the first name.") @Size(max = 80) String firstName,
        @NotBlank(message = "Enter the last name.") @Size(max = 80) String lastName,
        @NotEmpty(message = "Tick at least one role.") Set<UserRoleType> roles,
        @Size(max = 30) String employeeNumber,
        @Size(max = 40) String tscNumber,
        @Size(max = 30) String phone,
        @Size(max = 30) String admissionNumber) {
}
