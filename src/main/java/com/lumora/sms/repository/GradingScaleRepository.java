package com.lumora.sms.repository;

import com.lumora.sms.entity.GradingScale;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface GradingScaleRepository extends JpaRepository<GradingScale, Long> {

    Optional<GradingScale> findFirstByActiveTrue();

    Optional<GradingScale> findBySchoolIdAndName(Long schoolId, String name);
}
