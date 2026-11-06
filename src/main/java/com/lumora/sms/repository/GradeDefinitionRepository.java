package com.lumora.sms.repository;

import com.lumora.sms.entity.GradeDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface GradeDefinitionRepository extends JpaRepository<GradeDefinition, Long> {

    /** Ordered highest band first so the first matching band wins. */
    List<GradeDefinition> findByGradingScaleIdOrderByMinimumMarkDesc(Long gradingScaleId);
}
