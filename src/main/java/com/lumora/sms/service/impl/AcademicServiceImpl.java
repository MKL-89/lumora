package com.lumora.sms.service.impl;

import com.lumora.sms.entity.*;
import com.lumora.sms.exception.ResourceNotFoundException;
import com.lumora.sms.repository.*;
import com.lumora.sms.service.AcademicService;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

/** Central CRUD gateway for academic setup entities and teaching assignments. */
@Service
@org.springframework.validation.annotation.Validated
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AcademicServiceImpl implements AcademicService {

    private final EntityManager entityManager;
    private final SubjectRepository subjects;
    private final SchoolClassRepository schoolClasses;
    private final ClassStreamRepository streams;
    private final TeacherRepository teachers;
    private final TermRepository terms;
    private final AcademicYearRepository years;
    private final TeacherSubjectAssignmentRepository assignments;

    @Override
    @Transactional
    public <T extends BaseEntity> T save(T entity) {
        return entityManager.merge(entity);
    }

    @Override
    @Transactional
    public <T extends BaseEntity> void delete(Long id, Class<T> type) {
        T entity = entityManager.find(type, id);
        if (entity == null) {
            throw new ResourceNotFoundException(type.getSimpleName(), id);
        }
        entityManager.remove(entity);
    }

    @Override
    public List<Subject> subjects() {
        return subjects.findAllByOrderByNameAsc();
    }

    @Override
    @Transactional
    public TeacherSubjectAssignment assignTeacher(TeacherSubjectAssignment assignment) {
        return assignments.save(assignment);
    }

    @Override
    public List<SchoolClass> classes() {
        return schoolClasses.findAllByOrderByLevelAsc();
    }

    @Override
    public List<ClassStream> streams() {
        return streams.findAllByOrderBySchoolClassLevelAscNameAsc();
    }

    @Override
    public List<Teacher> teachers() {
        return teachers.findAllByOrderByLastNameAscFirstNameAsc();
    }

    @Override
    public java.util.Optional<Teacher> teacherForUser(String username) {
        return username == null ? java.util.Optional.empty() : teachers.findByUserUsername(username);
    }

    @Override
    public List<Term> terms() {
        return terms.findAllByOrderByAcademicYearNameAscTermNumberAsc();
    }

    @Override
    public List<AcademicYear> academicYears() {
        return years.findAllByOrderByNameDesc();
    }

    /** The term flagged active, or the most recent one when nothing is flagged. */
    @Override
    public Term activeTerm() {
        return terms.findFirstByActiveTrue()
                .orElseGet(() -> terms.findAllByOrderByAcademicYearNameAscTermNumberAsc().stream()
                        .reduce((first, second) -> second)
                        .orElse(null));
    }

    @Override
    public List<TeacherSubjectAssignment> assignmentsFor(String teacherUsername) {
        return assignments.findByTeacherUserUsername(teacherUsername);
    }
}
