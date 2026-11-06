package com.lumora.sms.service;

import com.lumora.sms.dto.MarkEntryRequest;
import com.lumora.sms.entity.*;
import com.lumora.sms.exception.BusinessRuleViolationException;
import com.lumora.sms.repository.*;
import com.lumora.sms.service.impl.ExaminationServiceImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Rules that protect the integrity of examination marks. These are the rules a
 * school would be most damaged by if they broke, so they are tested directly
 * rather than only through the web layer.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ExaminationService mark entry")
class ExaminationServiceImplTest {

    @Mock private ExaminationRepository examinations;
    @Mock private ExamSubjectRepository examSubjects;
    @Mock private ExamResultRepository results;
    @Mock private StudentRepository students;
    @Mock private TeacherSubjectAssignmentRepository assignments;
    @Mock private GradeDefinitionRepository gradeDefinitions;

    @InjectMocks private ExaminationServiceImpl service;

    private ExamSubject examSubject;
    private Examination examination;

    @BeforeEach
    void setUp() {
        School school = new School();
        school.setName("Lumora");

        GradingScale scale = new GradingScale();
        scale.setId(1L);
        scale.setSchool(school);
        scale.setName("KCSE Standard");

        SchoolClass form1 = new SchoolClass();
        form1.setSchool(school);
        form1.setName("Form 1");
        form1.setLevel(1);

        ClassStream stream = new ClassStream();
        stream.setId(10L);
        stream.setSchoolClass(form1);
        stream.setName("North");

        Term term = new Term();
        term.setId(20L);
        term.setName("Term 2");

        examination = new Examination();
        examination.setId(30L);
        examination.setClassStream(stream);
        examination.setTerm(term);
        examination.setGradingScale(scale);
        examination.setPublished(false);

        Subject maths = new Subject();
        maths.setId(40L);
        maths.setCode("MAT");
        maths.setName("Mathematics");

        examSubject = new ExamSubject();
        examSubject.setId(50L);
        examSubject.setExamination(examination);
        examSubject.setSubject(maths);
        examSubject.setMaximumMark(new BigDecimal("100"));

        Student student = new Student();
        student.setId(60L);
        student.setFirstName("Faith");
        student.setLastName("Wafula");

        when(examSubjects.findById(50L)).thenReturn(Optional.of(examSubject));
        when(students.findById(60L)).thenReturn(Optional.of(student));
        when(results.findByExamSubjectIdAndStudentId(anyLong(), anyLong())).thenReturn(Optional.empty());
        when(results.save(any(ExamResult.class))).thenAnswer(call -> call.getArgument(0));
        when(gradeDefinitions.findByGradingScaleIdOrderByMinimumMarkDesc(1L)).thenReturn(kcseBands());
        when(assignments.existsByTeacherIdAndSubjectIdAndClassStreamIdAndTermId(70L, 40L, 10L, 20L))
                .thenReturn(true);
    }

    /** A cut-down KCSE scale, ordered highest band first as the repository promises. */
    private List<GradeDefinition> kcseBands() {
        return List.of(
                band("A", 80, 100, 12),
                band("B", 65, 79, 9),
                band("C", 50, 64, 6),
                band("D", 35, 49, 3),
                band("E", 0, 34, 1));
    }

    private GradeDefinition band(String grade, int min, int max, int points) {
        GradeDefinition definition = new GradeDefinition();
        definition.setGrade(grade);
        definition.setMinimumMark(BigDecimal.valueOf(min));
        definition.setMaximumMark(BigDecimal.valueOf(max));
        definition.setPoints(points);
        return definition;
    }

    private MarkEntryRequest entry(long teacherId, String marks) {
        return new MarkEntryRequest(teacherId, 50L, 60L, new BigDecimal(marks), null);
    }

    @Nested
    @DisplayName("assigns a grade from the examination grading scale")
    class Grading {

        @Test
        void topOfTheScale() {
            ExamResult result = service.enterMark(entry(70L, "84"));
            assertThat(result.getGrade()).isEqualTo("A");
            assertThat(result.getPoints()).isEqualTo(12);
        }

        @Test
        void middleOfTheScale() {
            ExamResult result = service.enterMark(entry(70L, "58"));
            assertThat(result.getGrade()).isEqualTo("C");
            assertThat(result.getPoints()).isEqualTo(6);
        }

        @Test
        void bottomOfTheScale() {
            ExamResult result = service.enterMark(entry(70L, "12"));
            assertThat(result.getGrade()).isEqualTo("E");
            assertThat(result.getPoints()).isEqualTo(1);
        }

        @Test
        @DisplayName("exact band boundaries fall in the higher band")
        void boundariesAreInclusive() {
            assertThat(service.enterMark(entry(70L, "80")).getGrade()).isEqualTo("A");
            assertThat(service.enterMark(entry(70L, "79")).getGrade()).isEqualTo("B");
            assertThat(service.enterMark(entry(70L, "65")).getGrade()).isEqualTo("B");
        }

        @Test
        @DisplayName("marks are graded on their percentage of the paper maximum, not the raw score")
        void gradesAgainstThePaperMaximum() {
            examSubject.setMaximumMark(new BigDecimal("50"));

            // 42 out of 50 is 84%, which is an A even though 42 alone would be a D.
            ExamResult result = service.enterMark(entry(70L, "42"));
            assertThat(result.getGrade()).isEqualTo("A");
        }
    }

    @Nested
    @DisplayName("refuses invalid entries")
    class Rules {

        @Test
        void marksAboveThePaperMaximum() {
            assertThatThrownBy(() -> service.enterMark(entry(70L, "101")))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("cannot exceed");
        }

        @Test
        void negativeMarks() {
            assertThatThrownBy(() -> service.enterMark(entry(70L, "-1")))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("negative");
        }

        @Test
        @DisplayName("a teacher not assigned to the subject in that stream and term")
        void unassignedTeacher() {
            assertThatThrownBy(() -> service.enterMark(entry(99L, "70")))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("assigned to them");
        }

        @Test
        @DisplayName("any change once results are published")
        void publishedResultsAreImmutable() {
            examination.setPublished(true);

            assertThatThrownBy(() -> service.enterMark(entry(70L, "70")))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("published");

            verify(results, never()).save(any());
        }
    }
}
