package com.lumora.sms.entity;
import jakarta.persistence.*; import lombok.*;
@Entity @Table(name="grading_scales",uniqueConstraints=@UniqueConstraint(columnNames={"school_id","name"})) @Getter @Setter @NoArgsConstructor
public class GradingScale extends BaseEntity { @ManyToOne(optional=false) @JoinColumn(name="school_id") private School school; @Column(nullable=false,length=80) private String name; @Column(nullable=false) private boolean active=true; }
