package com.lumora.sms.entity;
import jakarta.persistence.*; import lombok.*; import java.time.LocalDateTime;
/** One-use, hashed password reset credential with a short expiry. */
@Entity @Table(name="password_reset_tokens") @Getter @Setter @NoArgsConstructor
public class PasswordResetToken extends BaseEntity { @ManyToOne(optional=false) @JoinColumn(name="user_id") private User user; @Column(nullable=false,unique=true,length=255) private String tokenHash; @Column(nullable=false) private LocalDateTime expiresAt; private LocalDateTime usedAt; }
