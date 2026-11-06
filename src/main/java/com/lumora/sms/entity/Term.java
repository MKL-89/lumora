package com.lumora.sms.entity;
import jakarta.persistence.*; import lombok.*; import java.time.LocalDate;
@Entity @Table(name="terms", uniqueConstraints=@UniqueConstraint(columnNames={"academic_year_id","term_number"})) @Getter @Setter @NoArgsConstructor
public class Term extends BaseEntity { @ManyToOne(optional=false) @JoinColumn(name="academic_year_id") private AcademicYear academicYear; @Column(name="term_number",nullable=false) private Integer termNumber; @Column(nullable=false,length=40) private String name; @Column(nullable=false) private LocalDate startDate; @Column(nullable=false) private LocalDate endDate; @Column(nullable=false) private boolean active; }
