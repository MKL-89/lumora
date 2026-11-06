package com.lumora.sms.entity;
import jakarta.persistence.*; import lombok.*;
@Entity @Table(name="book_copies") @Getter @Setter @NoArgsConstructor
public class BookCopy extends BaseEntity { @ManyToOne(optional=false) @JoinColumn(name="book_id") private Book book; @Column(nullable=false,unique=true,length=40) private String accessionNumber; @Column(nullable=false,length=30) private String status="AVAILABLE"; }
