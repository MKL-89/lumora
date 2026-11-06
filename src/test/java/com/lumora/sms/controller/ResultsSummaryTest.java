package com.lumora.sms.controller;

import com.lumora.sms.entity.*;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/** The per-examination summary line on the results lookup. */
@DisplayName("Results summary")
class ResultsSummaryTest {

    @Test
    @DisplayName("groups papers by examination and averages percentage and points")
    void summarises() {
        Examination opener = exam(1L, "Opener");
        Examination midterm = exam(2L, "Mid-term");

        var rows = List.of(
                result(opener, "80", "100", 11),
                result(opener, "30", "50", 7),     // 60%
                result(midterm, "45", "100", 5));

        var summary = ExaminationController.summarise(rows);

        assertThat(summary).hasSize(2);
        assertThat(summary.get(0).examination()).isEqualTo("Opener");
        assertThat(summary.get(0).papers()).isEqualTo(2);
        assertThat(summary.get(0).meanPercent()).isEqualByComparingTo("70.0");
        assertThat(summary.get(0).totalPoints()).isEqualTo(18);
        assertThat(summary.get(0).meanPoints()).isEqualByComparingTo("9.0");
        assertThat(summary.get(1).meanPercent()).isEqualByComparingTo("45.0");
    }

    @Test
    @DisplayName("leaves mean points blank when no paper has been graded")
    void ungraded() {
        var summary = ExaminationController.summarise(List.of(result(exam(1L, "Opener"), "40", "100", null)));

        assertThat(summary.get(0).meanPoints()).isNull();
        assertThat(summary.get(0).totalPoints()).isZero();
    }

    private static Examination exam(Long id, String name) {
        Examination e = new Examination();
        e.setId(id);
        e.setName(name);
        e.setExaminationDate(LocalDate.of(2026, 5, 20));
        return e;
    }

    private static ExamResult result(Examination exam, String marks, String max, Integer points) {
        ExamSubject paper = new ExamSubject();
        paper.setExamination(exam);
        paper.setMaximumMark(new BigDecimal(max));
        ExamResult r = new ExamResult();
        r.setExamSubject(paper);
        r.setMarks(new BigDecimal(marks));
        r.setPoints(points);
        return r;
    }
}
