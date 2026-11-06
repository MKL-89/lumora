package com.lumora.sms.security.reset;
import jakarta.validation.constraints.*;
/** Validated one-time-token password change request. */
public record ResetPasswordRequest(@NotBlank String token,@NotBlank @Size(min=12,max=100) String newPassword) { }
