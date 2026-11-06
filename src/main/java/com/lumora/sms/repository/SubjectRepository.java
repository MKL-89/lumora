package com.lumora.sms.repository;

import com.lumora.sms.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SubjectRepository extends JpaRepository<Subject, Long> {

    Optional<Subject> findBySchoolIdAndCode(Long schoolId, String code);

    List<Subject> findAllByOrderByNameAsc();
}
