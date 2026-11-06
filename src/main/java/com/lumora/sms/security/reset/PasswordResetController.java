package com.lumora.sms.security.reset;
import jakarta.validation.Valid; import jakarta.validation.constraints.Email; import lombok.RequiredArgsConstructor; import org.springframework.http.*; import org.springframework.validation.annotation.Validated; import org.springframework.web.bind.annotation.*;
/** Minimal public endpoints for requesting and completing a password reset. */
@RestController @Validated @RequestMapping("/password-reset") @RequiredArgsConstructor public class PasswordResetController { private final PasswordResetService resetService;
 @PostMapping("/request") public ResponseEntity<Void> request(@RequestParam @Email String email){resetService.request(email);return ResponseEntity.accepted().build();}
 @PostMapping("/confirm") public ResponseEntity<Void> confirm(@Valid @RequestBody ResetPasswordRequest request){resetService.reset(request);return ResponseEntity.noContent().build();}
}
