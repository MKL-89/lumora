package com.lumora.sms.repository;

import com.lumora.sms.entity.FeeCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface FeeCategoryRepository extends JpaRepository<FeeCategory, Long> {

    Optional<FeeCategory> findBySchoolIdAndName(Long schoolId, String name);

    List<FeeCategory> findByActiveTrueOrderByNameAsc();
}
