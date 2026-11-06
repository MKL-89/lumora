package com.lumora.sms.entity;
import jakarta.persistence.*; import lombok.*;
@Entity @Table(name="student_guardians", uniqueConstraints=@UniqueConstraint(columnNames={"student_id","parent_id"})) @Getter @Setter @NoArgsConstructor
public class StudentGuardian extends BaseEntity { @ManyToOne(optional=false) @JoinColumn(name="student_id") private Student student; @ManyToOne(optional=false) @JoinColumn(name="parent_id") private Parent parent; @Column(nullable=false,length=30) private String relationship; @Column(nullable=false) private boolean primaryContact; }
