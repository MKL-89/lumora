package com.lumora.sms.repository;

import com.lumora.sms.entity.FeeStructure;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface FeeStructureRepository extends JpaRepository<FeeStructure, Long> {

    Optional<FeeStructure> findByTermIdAndSchoolClassId(Long termId, Long schoolClassId);

    List<FeeStructure> findByTermId(Long termId);

    List<FeeStructure> findByActiveTrue();
}
