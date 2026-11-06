package com.lumora.sms.repository;

import com.lumora.sms.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {

    Optional<Student> findByAdmissionNumber(String admissionNumber);

    boolean existsByAdmissionNumber(String admissionNumber);

    boolean existsByIdAndUserUsername(Long id, String username);

    Optional<Student> findByUserUsername(String username);

    long countByActiveTrue();

    List<Student> findAllByOrderByAdmissionNumberAsc();

    /** Case-insensitive search across the name parts and the admission number. */
    @Query("""
           SELECT s FROM Student s
           WHERE LOWER(CONCAT(s.firstName, ' ', COALESCE(s.middleName, ''), ' ', s.lastName))
                 LIKE LOWER(CONCAT('%', :term, '%'))
              OR LOWER(s.admissionNumber) LIKE LOWER(CONCAT('%', :term, '%'))
           ORDER BY s.admissionNumber
           """)
    List<Student> search(@Param("term") String term);

    /** Learners actively enrolled in a stream; drives the attendance register. */
    @Query("""
           SELECT e.student FROM StudentEnrollment e
           WHERE e.classStream.id = :classStreamId AND e.active = true AND e.student.active = true
           ORDER BY e.student.admissionNumber
           """)
    List<Student> findEnrolledInStream(@Param("classStreamId") Long classStreamId);

    /** Gender split for the dashboard demographic chart. */
    @Query("SELECT s.gender, COUNT(s) FROM Student s WHERE s.active = true GROUP BY s.gender")
    List<Object[]> countByGender();
}
