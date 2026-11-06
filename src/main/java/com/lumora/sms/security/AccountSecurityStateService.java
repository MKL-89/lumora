package com.lumora.sms.security;
import com.lumora.sms.repository.UserRepository; import lombok.RequiredArgsConstructor; import org.springframework.beans.factory.annotation.Value; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional; import java.time.LocalDateTime;
/** Transactionally manages login counters and temporary account locks. */
@Service @RequiredArgsConstructor public class AccountSecurityStateService { private final UserRepository users; @Value("${app.security.lockout.max-attempts:5}") private int maxAttempts; @Value("${app.security.lockout.minutes:15}") private long lockoutMinutes;
 @Transactional public void recordSuccess(Long id){users.findById(id).ifPresent(u->{u.setFailedLoginAttempts(0);u.setLastFailedLoginAt(null);u.setLockoutUntil(null);});}
 /** True when the account still has a temporary password set by an administrator. */
 @Transactional(readOnly=true) public boolean mustChangePassword(Long id){return users.findById(id).map(com.lumora.sms.entity.User::isMustChangePassword).orElse(false);}
 @Transactional public void recordFailure(String username){users.findByUsername(username).ifPresent(u->{int attempts=u.getFailedLoginAttempts()+1;u.setFailedLoginAttempts(attempts);u.setLastFailedLoginAt(LocalDateTime.now());if(attempts>=maxAttempts)u.setLockoutUntil(LocalDateTime.now().plusMinutes(lockoutMinutes));});}
}
