package com.lumora.sms.entity;
import jakarta.persistence.*; import lombok.*; import java.time.LocalDateTime;
@Entity @Table(name="notification_recipients",uniqueConstraints=@UniqueConstraint(columnNames={"notification_id","user_id"})) @Getter @Setter @NoArgsConstructor
public class NotificationRecipient extends BaseEntity { @ManyToOne(optional=false) @JoinColumn(name="notification_id") private Notification notification; @ManyToOne(optional=false) @JoinColumn(name="user_id") private User user; private LocalDateTime readAt; }
