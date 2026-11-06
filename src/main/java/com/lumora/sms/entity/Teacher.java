package com.lumora.sms.entity;
import com.lumora.sms.enums.Gender; import jakarta.persistence.*; import lombok.*; import java.time.LocalDate;
@Entity @Table(name="teachers") @Getter @Setter @NoArgsConstructor
public class Teacher extends BaseEntity { @OneToOne(optional=false) @JoinColumn(name="user_id",unique=true) private User user; @Column(nullable=false,unique=true,length=30) private String employeeNumber; @Column(nullable=false,length=80) private String firstName; @Column(nullable=false,length=80) private String lastName; @Enumerated(EnumType.STRING) private Gender gender; private String phone; private String tscNumber; private LocalDate employmentDate; }
