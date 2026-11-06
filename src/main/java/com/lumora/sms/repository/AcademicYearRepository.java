package com.lumora.sms.repository;

import com.lumora.sms.entity.AcademicYear;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface AcademicYearRepository extends JpaRepository<AcademicYear, Long> {

    Optional<AcademicYear> findFirstByActiveTrue();

    Optional<AcademicYear> findBySchoolIdAndName(Long schoolId, String name);

    List<AcademicYear> findAllByOrderByNameDesc();
}
