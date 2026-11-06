package com.lumora.sms.entity;
import jakarta.persistence.*; import lombok.*;
@Entity @Table(name="parents") @Getter @Setter @NoArgsConstructor
public class Parent extends BaseEntity { @OneToOne @JoinColumn(name="user_id",unique=true) private User user; @Column(nullable=false,length=80) private String firstName; @Column(nullable=false,length=80) private String lastName; @Column(nullable=false,length=30) private String phone; private String nationalId; private String email; private String occupation; }
