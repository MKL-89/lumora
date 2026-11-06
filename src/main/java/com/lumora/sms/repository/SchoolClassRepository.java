package com.lumora.sms.repository;

import com.lumora.sms.entity.SchoolClass;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SchoolClassRepository extends JpaRepository<SchoolClass, Long> {

    Optional<SchoolClass> findBySchoolIdAndName(Long schoolId, String name);

    List<SchoolClass> findAllByOrderByLevelAsc();
}
