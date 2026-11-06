package com.lumora.sms.repository;

import com.lumora.sms.entity.ExamResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface ExamResultRepository extends JpaRepository<ExamResult, Long> {

    Optional<ExamResult> findByExamSubjectIdAndStudentId(Long examSubjectId, Long studentId);

    List<ExamResult> findByExamSubjectIdOrderByMarksDesc(Long examSubjectId);

    List<ExamResult> findByStudentIdOrderByIdDesc(Long studentId);

    /** Published results only, so learners never see marks before release. */
    @Query("""
           SELECT r FROM ExamResult r
           WHERE r.student.id = :studentId AND r.examSubject.examination.published = true
           ORDER BY r.examSubject.examination.examinationDate DESC, r.examSubject.subject.name
           """)
    List<ExamResult> findPublishedForStudent(@Param("studentId") Long studentId);

    /** Mean mark per subject across an examination; drives the performance chart. */
    @Query("""
           SELECT r.examSubject.subject.name, AVG(r.marks)
           FROM ExamResult r
           WHERE r.examSubject.examination.id = :examinationId
           GROUP BY r.examSubject.subject.name
           ORDER BY r.examSubject.subject.name
           """)
    List<Object[]> averageBySubject(@Param("examinationId") Long examinationId);

    /** School-wide mean per subject, used when no single examination is selected. */
    @Query("""
           SELECT r.examSubject.subject.name, AVG(r.marks)
           FROM ExamResult r
           GROUP BY r.examSubject.subject.name
           ORDER BY r.examSubject.subject.name
           """)
    List<Object[]> averageBySubjectOverall();

    /** Grade distribution for the results screen. */
    @Query("""
           SELECT r.grade, COUNT(r) FROM ExamResult r
           WHERE r.grade IS NOT NULL
           GROUP BY r.grade ORDER BY r.grade
           """)
    List<Object[]> gradeDistribution();
}
