package com.lumora.sms.repository;

import com.lumora.sms.entity.FeeStructureItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.util.List;

public interface FeeStructureItemRepository extends JpaRepository<FeeStructureItem, Long> {

    List<FeeStructureItem> findByFeeStructureId(Long feeStructureId);

    @Query("SELECT COALESCE(SUM(i.amount), 0) FROM FeeStructureItem i WHERE i.feeStructure.id = :feeStructureId")
    BigDecimal totalFor(@Param("feeStructureId") Long feeStructureId);
}
