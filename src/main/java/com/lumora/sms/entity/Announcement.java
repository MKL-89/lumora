package com.lumora.sms.entity;
import jakarta.persistence.*; import lombok.*; import java.time.LocalDateTime;
@Entity @Table(name="announcements") @Getter @Setter @NoArgsConstructor
public class Announcement extends BaseEntity { @ManyToOne(optional=false) @JoinColumn(name="created_by_user_id") private User createdBy; @Column(nullable=false,length=150) private String title; @Lob @Column(nullable=false) private String content; @Column(nullable=false) private LocalDateTime publishedAt; private LocalDateTime expiresAt; }
