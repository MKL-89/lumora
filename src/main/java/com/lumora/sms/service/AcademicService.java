package com.lumora.sms.service;

import com.lumora.sms.entity.*;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/** Provides CRUD-style management of the academic calendar and teaching allocations. */
public interface AcademicService {

    <T extends BaseEntity> T save(@NotNull T entity);

    <T extends BaseEntity> void delete(@NotNull Long id, @NotNull Class<T> type);

    List<Subject> subjects();

    TeacherSubjectAssignment assignTeacher(@NotNull TeacherSubjectAssignment assignment);

    List<SchoolClass> classes();

    List<ClassStream> streams();

    List<Teacher> teachers();

    /** The teacher record behind a signed-in staff account, if it has one. */
    java.util.Optional<Teacher> teacherForUser(String username);

    List<Term> terms();

    List<AcademicYear> academicYears();

    Term activeTerm();

    List<TeacherSubjectAssignment> assignmentsFor(String teacherUsername);
}
