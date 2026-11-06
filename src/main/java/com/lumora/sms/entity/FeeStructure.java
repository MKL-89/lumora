package com.lumora.sms.entity;
import jakarta.persistence.*; import lombok.*;
@Entity @Table(name="fee_structures",uniqueConstraints=@UniqueConstraint(columnNames={"term_id","school_class_id"})) @Getter @Setter @NoArgsConstructor
public class FeeStructure extends BaseEntity { @ManyToOne(optional=false) @JoinColumn(name="term_id") private Term term; @ManyToOne(optional=false) @JoinColumn(name="school_class_id") private SchoolClass schoolClass; @Column(nullable=false,length=3) private String currencyCode="KES"; @Column(nullable=false) private boolean active=true; }
