package com.lumora.sms.repository;

import com.lumora.sms.entity.TeacherSubjectAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TeacherSubjectAssignmentRepository extends JpaRepository<TeacherSubjectAssignment, Long> {

    boolean existsByTeacherIdAndSubjectIdAndClassStreamIdAndTermId(
            Long teacherId, Long subjectId, Long classStreamId, Long termId);

    List<TeacherSubjectAssignment> findByTeacherId(Long teacherId);

    List<TeacherSubjectAssignment> findByClassStreamId(Long classStreamId);

    List<TeacherSubjectAssignment> findByTeacherUserUsername(String username);
}
