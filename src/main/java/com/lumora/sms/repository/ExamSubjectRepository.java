package com.lumora.sms.repository;

import com.lumora.sms.entity.ExamSubject;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ExamSubjectRepository extends JpaRepository<ExamSubject, Long> {

    List<ExamSubject> findByExaminationIdOrderBySubjectNameAsc(Long examinationId);

    Optional<ExamSubject> findByExaminationIdAndSubjectId(Long examinationId, Long subjectId);
}
