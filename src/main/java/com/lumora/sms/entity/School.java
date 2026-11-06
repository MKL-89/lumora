package com.lumora.sms.entity;
import jakarta.persistence.*; import lombok.*;
@Entity @Table(name="schools") @Getter @Setter @NoArgsConstructor
public class School extends BaseEntity { @Column(nullable=false,length=150) private String name; @Column(nullable=false,unique=true,length=30) private String code; private String registrationNumber; private String county; private String address; private String phone; private String email; }
