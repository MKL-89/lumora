package com.lumora.sms.entity;
import jakarta.persistence.*; import lombok.*;
@Entity @Table(name="teacher_subject_assignments",uniqueConstraints=@UniqueConstraint(columnNames={"teacher_id","subject_id","class_stream_id","term_id"})) @Getter @Setter @NoArgsConstructor
public class TeacherSubjectAssignment extends BaseEntity { @ManyToOne(optional=false) @JoinColumn(name="teacher_id") private Teacher teacher; @ManyToOne(optional=false) @JoinColumn(name="subject_id") private Subject subject; @ManyToOne(optional=false) @JoinColumn(name="class_stream_id") private ClassStream classStream; @ManyToOne(optional=false) @JoinColumn(name="term_id") private Term term; }
