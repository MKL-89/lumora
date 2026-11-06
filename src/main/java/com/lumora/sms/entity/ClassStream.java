package com.lumora.sms.entity;
import jakarta.persistence.*; import lombok.*;
@Entity @Table(name="class_streams", uniqueConstraints=@UniqueConstraint(columnNames={"school_class_id","name"})) @Getter @Setter @NoArgsConstructor
public class ClassStream extends BaseEntity { @ManyToOne(optional=false) @JoinColumn(name="school_class_id") private SchoolClass schoolClass; @Column(nullable=false,length=40) private String name; private Integer capacity; }
