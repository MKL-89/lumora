package com.lumora.sms.entity;
import jakarta.persistence.*; import lombok.*;
@Entity @Table(name="books") @Getter @Setter @NoArgsConstructor
public class Book extends BaseEntity { @ManyToOne(optional=false) @JoinColumn(name="book_category_id") private BookCategory category; @Column(nullable=false,length=255) private String title; @Column(nullable=false,length=150) private String author; @Column(unique=true,length=30) private String isbn; private String publisher; private Integer publicationYear; }
