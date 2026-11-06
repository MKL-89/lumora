package com.lumora.sms.repository;

import com.lumora.sms.entity.StudentGuardian;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface StudentGuardianRepository extends JpaRepository<StudentGuardian, Long> {

    boolean existsByStudentIdAndParentUserUsername(Long studentId, String username);

    List<StudentGuardian> findByStudentId(Long studentId);

    List<StudentGuardian> findByParentUserUsername(String username);
}
