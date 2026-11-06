package com.lumora.sms.entity;
import jakarta.persistence.*; import lombok.*;
@Entity @Table(name="fee_categories",uniqueConstraints=@UniqueConstraint(columnNames={"school_id","name"})) @Getter @Setter @NoArgsConstructor
public class FeeCategory extends BaseEntity { @ManyToOne(optional=false) @JoinColumn(name="school_id") private School school; @Column(nullable=false,length=100) private String name; private String description; @Column(nullable=false) private boolean active=true; }
