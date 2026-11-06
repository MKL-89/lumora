package com.lumora.sms.entity;
import jakarta.persistence.*; import lombok.*; import java.math.BigDecimal;
@Entity @Table(name="fee_structure_items",uniqueConstraints=@UniqueConstraint(columnNames={"fee_structure_id","fee_category_id"})) @Getter @Setter @NoArgsConstructor
public class FeeStructureItem extends BaseEntity { @ManyToOne(optional=false) @JoinColumn(name="fee_structure_id") private FeeStructure feeStructure; @ManyToOne(optional=false) @JoinColumn(name="fee_category_id") private FeeCategory feeCategory; @Column(nullable=false,precision=15,scale=2) private BigDecimal amount; }
