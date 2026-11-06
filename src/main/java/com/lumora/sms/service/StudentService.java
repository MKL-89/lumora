package com.lumora.sms.service;

import com.lumora.sms.dto.*;
import com.lumora.sms.entity.Student;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/** Owns admission, enrollment, and privacy-safe student access operations. */
public interface StudentService {

    StudentResponse admit(@Valid StudentAdmissionRequest request);

    StudentResponse get(@NotNull Long id);

    List<StudentResponse> list();

    void deactivate(@NotNull Long id);

    void reactivate(@NotNull Long id);

    List<StudentResponse> search(String term);

    /** The full student screen: placement, fees, results, loans, attendance. */
    StudentProfile profile(@NotNull Long id);

    /** Resolves the Student record behind a signed-in student account. */
    Student requireByUsername(String username);

    /** The learner with this admission number, ignoring case and surrounding spaces. */
    java.util.Optional<Student> findByAdmissionNumber(String admissionNumber);

    List<ChartPoint> genderSplit();

    List<ChartPoint> enrolmentByStream();
}
