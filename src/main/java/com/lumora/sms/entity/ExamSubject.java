package com.lumora.sms.entity;
import jakarta.persistence.*; import lombok.*; import java.math.BigDecimal;
@Entity @Table(name="exam_subjects",uniqueConstraints=@UniqueConstraint(columnNames={"examination_id","subject_id"})) @Getter @Setter @NoArgsConstructor
public class ExamSubject extends BaseEntity { @ManyToOne(optional=false) @JoinColumn(name="examination_id") private Examination examination; @ManyToOne(optional=false) @JoinColumn(name="subject_id") private Subject subject; @Column(nullable=false,precision=6,scale=2) private BigDecimal maximumMark; }
