package com.lumora.sms.repository;
import com.lumora.sms.entity.PasswordResetToken; import org.springframework.data.jpa.repository.JpaRepository; import java.time.LocalDateTime; import java.util.*;
/** Accesses hashed reset credentials; raw tokens are never persisted. */
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken,Long> { Optional<PasswordResetToken> findByTokenHashAndUsedAtIsNull(String tokenHash); List<PasswordResetToken> findByUserIdAndUsedAtIsNull(Long userId); long deleteByExpiresAtBefore(LocalDateTime instant); }
