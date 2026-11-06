package com.lumora.sms.entity;
import jakarta.persistence.*; import lombok.*; import java.math.BigDecimal; import java.time.LocalDateTime;
@Entity @Table(name="mpesa_transactions") @Getter @Setter @NoArgsConstructor
public class MpesaTransaction extends BaseEntity { @OneToOne(optional=false) @JoinColumn(name="payment_id",unique=true) private Payment payment; @Column(nullable=false,unique=true,length=100) private String transactionCode; @Column(nullable=false,length=30) private String payerPhone; @Column(nullable=false,precision=15,scale=2) private BigDecimal amount; @Column(nullable=false) private LocalDateTime transactionTime; @Column(length=100) private String accountReference; }
