package com.lumora.sms.entity;
import jakarta.persistence.*; import lombok.*; import java.math.BigDecimal;
@Entity @Table(name="exam_results",uniqueConstraints=@UniqueConstraint(columnNames={"exam_subject_id","student_id"})) @Getter @Setter @NoArgsConstructor
public class ExamResult extends BaseEntity { @ManyToOne(optional=false) @JoinColumn(name="exam_subject_id") private ExamSubject examSubject; @ManyToOne(optional=false) @JoinColumn(name="student_id") private Student student; @Column(nullable=false,precision=6,scale=2) private BigDecimal marks; @Column(length=5) private String grade; private Integer points; @Column(length=255) private String remarks; }
