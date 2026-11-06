package com.lumora.sms.entity;
import com.lumora.sms.enums.UserRoleType; import jakarta.persistence.*; import lombok.*;
@Entity @Table(name="roles") @Getter @Setter @NoArgsConstructor
public class Role extends BaseEntity { @Enumerated(EnumType.STRING) @Column(nullable=false,unique=true,length=40) private UserRoleType name; @Column(length=255) private String description; }
