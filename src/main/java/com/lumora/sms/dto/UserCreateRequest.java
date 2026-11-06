package com.lumora.sms.dto;
import com.lumora.sms.enums.UserRoleType; import jakarta.validation.constraints.*; import java.util.Set;
/** Validated input for a new sign-in account. */
public record UserCreateRequest(@NotBlank @Size(max=80) String username, @NotBlank @Email @Size(max=150) String email, @NotBlank @Size(min=8,max=100) String rawPassword, @NotEmpty Set<UserRoleType> roles) { }
