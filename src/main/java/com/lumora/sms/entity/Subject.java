package com.lumora.sms.entity;
import jakarta.persistence.*; import lombok.*;
@Entity @Table(name="subjects", uniqueConstraints=@UniqueConstraint(columnNames={"school_id","code"})) @Getter @Setter @NoArgsConstructor
public class Subject extends BaseEntity { @ManyToOne(optional=false) @JoinColumn(name="school_id") private School school; @Column(nullable=false,length=20) private String code; @Column(nullable=false,length=100) private String name; @Column(nullable=false) private boolean compulsory; }
