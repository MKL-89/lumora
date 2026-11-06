package com.lumora.sms.entity;
import jakarta.persistence.*; import lombok.*; import java.time.LocalDateTime;
@Entity @Table(name="audit_logs") @Getter @Setter @NoArgsConstructor
public class AuditLog extends BaseEntity { @ManyToOne @JoinColumn(name="user_id") private User user; @Column(nullable=false,length=80) private String action; @Column(nullable=false,length=80) private String entityName; private Long entityId; @Column(length=45) private String ipAddress; @Column(nullable=false) private LocalDateTime occurredAt; }
