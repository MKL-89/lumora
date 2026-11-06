package com.lumora.sms.entity;
import com.lumora.sms.enums.AttendanceStatus; import jakarta.persistence.*; import lombok.*;
@Entity @Table(name="attendance_records",uniqueConstraints=@UniqueConstraint(columnNames={"attendance_session_id","student_id"})) @Getter @Setter @NoArgsConstructor
public class AttendanceRecord extends BaseEntity { @ManyToOne(optional=false) @JoinColumn(name="attendance_session_id") private AttendanceSession attendanceSession; @ManyToOne(optional=false) @JoinColumn(name="student_id") private Student student; @Enumerated(EnumType.STRING) @Column(nullable=false,length=15) private AttendanceStatus status; @Column(length=255) private String remarks; }
