package com.lumora.sms.service.impl;

import com.lumora.sms.dto.*;
import com.lumora.sms.entity.*;
import com.lumora.sms.exception.*;
import com.lumora.sms.repository.*;
import com.lumora.sms.service.ExaminationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Applies examination rules: teacher-to-subject authorization, the configured
 * maximum mark, an immutable published result set, and automatic grading against
 * the examination's grading scale.
 */
@Service
@org.springframework.validation.annotation.Validated
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExaminationServiceImpl implements ExaminationService {

    private static final BigDecimal PERCENT = BigDecimal.valueOf(100);

    private final ExaminationRepository examinations;
    private final ExamSubjectRepository examSubjects;
    private final ExamResultRepository results;
    private final StudentRepository students;
    private final TeacherSubjectAssignmentRepository assignments;
    private final GradeDefinitionRepository gradeDefinitions;

    @Override
    @Transactional
    public Examination saveExamination(Examination exam) {
        return examinations.save(exam);
    }

    @Override
    @Transactional
    public ExamResult enterMark(@Valid MarkEntryRequest r) {
        ExamSubject examSubject = examSubjects.findById(r.examSubjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Exam subject", r.examSubjectId()));
        Examination exam = examSubject.getExamination();

        if (exam.isPublished()) {
            throw new BusinessRuleViolationException("Marks cannot be changed after results are published.");
        }
        if (r.marks().compareTo(examSubject.getMaximumMark()) > 0) {
            throw new BusinessRuleViolationException("Marks cannot exceed the examination maximum of "
                    + examSubject.getMaximumMark() + ".");
        }
        if (r.marks().signum() < 0) {
            throw new BusinessRuleViolationException("Marks cannot be negative.");
        }
        if (!assignments.existsByTeacherIdAndSubjectIdAndClassStreamIdAndTermId(
                r.teacherId(), examSubject.getSubject().getId(),
                exam.getClassStream().getId(), exam.getTerm().getId())) {
            throw new BusinessRuleViolationException("Teachers can only enter marks for subjects assigned to them.");
        }

        Student student = students.findById(r.studentId())
                .orElseThrow(() -> new ResourceNotFoundException("Student", r.studentId()));

        ExamResult result = results.findByExamSubjectIdAndStudentId(examSubject.getId(), student.getId())
                .orElseGet(ExamResult::new);
        result.setExamSubject(examSubject);
        result.setStudent(student);
        result.setMarks(r.marks());
        result.setRemarks(r.remarks());
        applyGrade(result, examSubject, exam.getGradingScale());
        return results.save(result);
    }

    /**
     * Converts the raw mark to a percentage of the paper's maximum, then assigns
     * the highest grade band whose range contains it. Bands are read newest-first
     * by minimum mark so the first match is always the correct one.
     */
    private void applyGrade(ExamResult result, ExamSubject examSubject, GradingScale scale) {
        if (scale == null || examSubject.getMaximumMark().signum() == 0) {
            return;
        }
        BigDecimal percentage = result.getMarks()
                .multiply(PERCENT)
                .divide(examSubject.getMaximumMark(), 2, RoundingMode.HALF_UP);

        for (GradeDefinition band : gradeDefinitions.findByGradingScaleIdOrderByMinimumMarkDesc(scale.getId())) {
            if (percentage.compareTo(band.getMinimumMark()) >= 0 && percentage.compareTo(band.getMaximumMark()) <= 0) {
                result.setGrade(band.getGrade());
                result.setPoints(band.getPoints());
                return;
            }
        }
        // No band matched (a gap in the scale); leave the mark ungraded rather than guess.
        result.setGrade(null);
        result.setPoints(null);
    }

    @Override
    @Transactional
    public void publish(Long id) {
        examinations.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Examination", id))
                .setPublished(true);
    }

    @Override
    public List<Examination> list() {
        return examinations.findAllByOrderByExaminationDateDesc();
    }

    @Override
    public Examination get(Long id) {
        return examinations.findById(id).orElseThrow(() -> new ResourceNotFoundException("Examination", id));
    }

    @Override
    public List<ExamSubject> subjectsOf(Long examinationId) {
        return examSubjects.findByExaminationIdOrderBySubjectNameAsc(examinationId);
    }

    @Override
    public ExamSubject examSubject(Long examSubjectId) {
        return examSubjects.findById(examSubjectId)
                .orElseThrow(() -> new ResourceNotFoundException("Exam subject", examSubjectId));
    }

    @Override
    public List<MarkSheetRow> markSheet(Long examSubjectId) {
        ExamSubject examSubject = examSubject(examSubjectId);
        Map<Long, ExamResult> byStudent = results.findByExamSubjectIdOrderByMarksDesc(examSubjectId).stream()
                .collect(Collectors.toMap(r -> r.getStudent().getId(), Function.identity(), (a, b) -> a));

        List<MarkSheetRow> rows = new ArrayList<>();
        for (Student student : students.findEnrolledInStream(examSubject.getExamination().getClassStream().getId())) {
            ExamResult existing = byStudent.get(student.getId());
            rows.add(new MarkSheetRow(
                    student.getId(),
                    student.getAdmissionNumber(),
                    fullName(student),
                    existing == null ? null : existing.getMarks(),
                    existing == null ? null : existing.getGrade(),
                    existing == null ? null : existing.getPoints()));
        }
        return rows;
    }

    @Override
    public List<ChartPoint> averageBySubject(Long examinationId) {
        List<Object[]> rows = examinationId == null
                ? results.averageBySubjectOverall()
                : results.averageBySubject(examinationId);
        return rows.stream()
                .map(row -> ChartPoint.of((String) row[0], (Number) row[1]))
                .toList();
    }

    @Override
    public List<ChartPoint> gradeDistribution() {
        return results.gradeDistribution().stream()
                .map(row -> ChartPoint.of((String) row[0], (Number) row[1]))
                .toList();
    }

    @Override
    public List<ExamResult> publishedResultsFor(Long studentId) {
        return results.findPublishedForStudent(studentId);
    }

    private String fullName(Student s) {
        return s.getMiddleName() == null || s.getMiddleName().isBlank()
                ? s.getFirstName() + " " + s.getLastName()
                : s.getFirstName() + " " + s.getMiddleName() + " " + s.getLastName();
    }
}
