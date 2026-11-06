package com.lumora.sms.entity;
import jakarta.persistence.*; import lombok.*; import java.time.LocalDateTime;
@Entity @Table(name="notifications") @Getter @Setter @NoArgsConstructor
public class Notification extends BaseEntity { @Column(nullable=false,length=150) private String title; @Lob @Column(nullable=false) private String message; @Column(nullable=false,length=30) private String type; @Column(nullable=false) private LocalDateTime sentAt; }
