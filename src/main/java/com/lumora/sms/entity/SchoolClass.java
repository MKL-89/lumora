package com.lumora.sms.entity;
import jakarta.persistence.*; import lombok.*;
@Entity @Table(name="school_classes", uniqueConstraints=@UniqueConstraint(columnNames={"school_id","name"})) @Getter @Setter @NoArgsConstructor
public class SchoolClass extends BaseEntity { @ManyToOne(optional=false) @JoinColumn(name="school_id") private School school; @Column(nullable=false,length=40) private String name; @Column(nullable=false) private Integer level; }
