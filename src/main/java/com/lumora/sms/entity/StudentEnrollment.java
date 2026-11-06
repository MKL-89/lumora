package com.lumora.sms.entity;
import jakarta.persistence.*; import lombok.*;
@Entity @Table(name="student_enrollments", uniqueConstraints=@UniqueConstraint(columnNames={"student_id","academic_year_id"})) @Getter @Setter @NoArgsConstructor
public class StudentEnrollment extends BaseEntity { @ManyToOne(optional=false) @JoinColumn(name="student_id") private Student student; @ManyToOne(optional=false) @JoinColumn(name="academic_year_id") private AcademicYear academicYear; @ManyToOne(optional=false) @JoinColumn(name="class_stream_id") private ClassStream classStream; @Column(nullable=false) private boolean active=true; }
