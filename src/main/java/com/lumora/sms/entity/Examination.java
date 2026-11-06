package com.lumora.sms.entity;
import jakarta.persistence.*; import lombok.*; import java.time.LocalDate;
@Entity @Table(name="examinations") @Getter @Setter @NoArgsConstructor
public class Examination extends BaseEntity { @ManyToOne(optional=false) @JoinColumn(name="term_id") private Term term; @ManyToOne(optional=false) @JoinColumn(name="class_stream_id") private ClassStream classStream; @ManyToOne(optional=false) @JoinColumn(name="grading_scale_id") private GradingScale gradingScale; @Column(nullable=false,length=100) private String name; @Column(nullable=false) private LocalDate examinationDate; @Column(nullable=false) private boolean published=false; }
