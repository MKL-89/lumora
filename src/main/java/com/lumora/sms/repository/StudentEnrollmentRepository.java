package com.lumora.sms.repository;

import com.lumora.sms.entity.StudentEnrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface StudentEnrollmentRepository extends JpaRepository<StudentEnrollment, Long> {

    Optional<StudentEnrollment> findByStudentIdAndActiveTrue(Long studentId);

    List<StudentEnrollment> findByClassStreamIdAndActiveTrue(Long classStreamId);

    long countByClassStreamIdAndActiveTrue(Long classStreamId);

    /** Head count per stream for the enrolment chart. */
    @Query("""
           SELECT e.classStream.schoolClass.name, e.classStream.name, COUNT(e)
           FROM StudentEnrollment e
           WHERE e.active = true
           GROUP BY e.classStream.schoolClass.name, e.classStream.name
           ORDER BY e.classStream.schoolClass.name, e.classStream.name
           """)
    List<Object[]> countPerStream();
}
