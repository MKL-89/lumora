package com.lumora.sms.entity;
import jakarta.persistence.*; import lombok.*; import java.time.LocalDate;
@Entity @Table(name="attendance_sessions",uniqueConstraints=@UniqueConstraint(columnNames={"class_stream_id","attendance_date"})) @Getter @Setter @NoArgsConstructor
public class AttendanceSession extends BaseEntity { @ManyToOne(optional=false) @JoinColumn(name="class_stream_id") private ClassStream classStream; @ManyToOne(optional=false) @JoinColumn(name="teacher_id") private Teacher recordedBy; @Column(name="attendance_date",nullable=false) private LocalDate attendanceDate; }
