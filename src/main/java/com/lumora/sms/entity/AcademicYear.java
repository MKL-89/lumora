package com.lumora.sms.entity;
import jakarta.persistence.*; import lombok.*; import java.time.LocalDate;
@Entity @Table(name="academic_years", uniqueConstraints=@UniqueConstraint(columnNames={"school_id","name"})) @Getter @Setter @NoArgsConstructor
public class AcademicYear extends BaseEntity { @ManyToOne(optional=false) @JoinColumn(name="school_id") private School school; @Column(nullable=false,length=20) private String name; @Column(nullable=false) private LocalDate startDate; @Column(nullable=false) private LocalDate endDate; @Column(nullable=false) private boolean active; }
