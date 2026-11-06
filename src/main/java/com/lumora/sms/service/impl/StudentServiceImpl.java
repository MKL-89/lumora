package com.lumora.sms.service.impl;

import com.lumora.sms.dto.*;
import com.lumora.sms.entity.*;
import com.lumora.sms.exception.*;
import com.lumora.sms.mapper.StudentMapper;
import com.lumora.sms.repository.*;
import com.lumora.sms.service.AttendanceService;
import com.lumora.sms.service.StudentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;

/** Admissions implementation that makes class placement mandatory and atomic. */
@Service
@org.springframework.validation.annotation.Validated
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentServiceImpl implements StudentService {

    private final StudentRepository students;
    private final SchoolRepository schools;
    private final AcademicYearRepository years;
    private final ClassStreamRepository streams;
    private final StudentEnrollmentRepository enrollments;
    private final StudentInvoiceRepository invoices;
    private final ExamResultRepository results;
    private final BookLoanRepository loans;
    private final AttendanceService attendance;
    private final StudentMapper mapper;

    @Override
    public StudentResponse get(Long id) {
        return mapper.toResponse(students.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student", id)));
    }

    @Override
    public List<StudentResponse> list() {
        return students.findAllByOrderByAdmissionNumberAsc().stream().map(mapper::toResponse).toList();
    }

    @Override
    public List<StudentResponse> search(String term) {
        if (term == null || term.isBlank()) {
            return list();
        }
        return students.search(term.trim()).stream().map(mapper::toResponse).toList();
    }

    /**
     * Admission and class placement happen together: a learner without a stream
     * cannot be registered, marked present, or examined, so the two writes share
     * one transaction.
     */
    @Override
    @Transactional
    public StudentResponse admit(@Valid StudentAdmissionRequest r) {
        if (students.existsByAdmissionNumber(r.admissionNumber())) {
            throw new DuplicateResourceException("Admission number", r.admissionNumber());
        }
        School school = schools.findById(r.schoolId())
                .orElseThrow(() -> new ResourceNotFoundException("School", r.schoolId()));
        AcademicYear year = years.findById(r.academicYearId())
                .orElseThrow(() -> new ResourceNotFoundException("Academic year", r.academicYearId()));
        ClassStream stream = streams.findById(r.classStreamId())
                .orElseThrow(() -> new ResourceNotFoundException("Class stream", r.classStreamId()));

        if (!stream.getSchoolClass().getSchool().getId().equals(school.getId())
                || !year.getSchool().getId().equals(school.getId())) {
            throw new BusinessRuleViolationException(
                    "The class stream and academic year must belong to the student's school.");
        }
        if (r.admissionDate() != null && r.admissionDate().isBefore(r.dateOfBirth())) {
            throw new BusinessRuleViolationException("A learner cannot be admitted before they were born.");
        }

        Student student = new Student();
        student.setSchool(school);
        student.setAdmissionNumber(r.admissionNumber());
        student.setFirstName(r.firstName());
        student.setMiddleName(r.middleName());
        student.setLastName(r.lastName());
        student.setGender(r.gender());
        student.setDateOfBirth(r.dateOfBirth());
        student.setAdmissionDate(r.admissionDate());
        student = students.save(student);

        StudentEnrollment enrollment = new StudentEnrollment();
        enrollment.setStudent(student);
        enrollment.setAcademicYear(year);
        enrollment.setClassStream(stream);
        enrollments.save(enrollment);

        return mapper.toResponse(student);
    }

    @Override
    @Transactional
    public void deactivate(Long id) {
        students.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student", id))
                .setActive(false);
    }

    @Override
    @Transactional
    public void reactivate(Long id) {
        students.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student", id))
                .setActive(true);
    }

    @Override
    public StudentProfile profile(Long id) {
        Student student = students.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student", id));
        StudentEnrollment enrollment = enrollments.findByStudentIdAndActiveTrue(id).orElse(null);

        List<StudentInvoice> studentInvoices = invoices.findByStudentIdOrderByDueDateDesc(id);
        BigDecimal outstanding = studentInvoices.stream()
                .map(StudentInvoice::getBalanceAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new StudentProfile(
                student,
                enrollment == null ? null : enrollment.getClassStream().getSchoolClass().getName(),
                enrollment == null ? null : enrollment.getClassStream().getName(),
                enrollment == null ? null : enrollment.getAcademicYear().getName(),
                studentInvoices,
                results.findByStudentIdOrderByIdDesc(id),
                student.getUser() == null ? List.of() : loans.findByBorrowerIdOrderByBorrowedOnDesc(student.getUser().getId()),
                attendance.summaryFor(id),
                outstanding);
    }

    @Override
    public Student requireByUsername(String username) {
        return students.findByUserUsername(username)
                .orElseThrow(() -> new BusinessRuleViolationException(
                        "This account is not linked to a student record."));
    }

    @Override
    public List<ChartPoint> genderSplit() {
        return students.countByGender().stream()
                .map(row -> ChartPoint.of(String.valueOf(row[0]), (Number) row[1]))
                .toList();
    }

    @Override
    public List<ChartPoint> enrolmentByStream() {
        return enrollments.countPerStream().stream()
                .map(row -> ChartPoint.of(row[0] + " " + row[1], (Number) row[2]))
                .toList();
    }
}
