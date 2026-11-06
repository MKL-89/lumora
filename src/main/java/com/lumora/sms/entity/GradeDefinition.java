package com.lumora.sms.entity;
import jakarta.persistence.*; import lombok.*; import java.math.BigDecimal;
@Entity @Table(name="grade_definitions",uniqueConstraints=@UniqueConstraint(columnNames={"grading_scale_id","grade"})) @Getter @Setter @NoArgsConstructor
public class GradeDefinition extends BaseEntity { @ManyToOne(optional=false) @JoinColumn(name="grading_scale_id") private GradingScale gradingScale; @Column(nullable=false,length=5) private String grade; @Column(nullable=false,precision=5,scale=2) private BigDecimal minimumMark; @Column(nullable=false,precision=5,scale=2) private BigDecimal maximumMark; @Column(nullable=false) private Integer points; @Column(length=100) private String remarks; }
