package com.lumora.sms.service;

import com.lumora.sms.dto.*;
import com.lumora.sms.entity.*;
import jakarta.validation.Valid;
import java.util.List;

/** Creates examinations, applies authorization-aware mark entry, and grades results. */
public interface ExaminationService {

    Examination saveExamination(Examination examination);

    ExamResult enterMark(@Valid MarkEntryRequest request);

    void publish(Long examinationId);

    List<Examination> list();

    Examination get(Long examinationId);

    List<ExamSubject> subjectsOf(Long examinationId);

    ExamSubject examSubject(Long examSubjectId);

    /** Every candidate in the examination's stream, with any mark already recorded. */
    List<MarkSheetRow> markSheet(Long examSubjectId);

    /** Mean mark per subject; falls back to school-wide figures when id is null. */
    List<ChartPoint> averageBySubject(Long examinationId);

    List<ChartPoint> gradeDistribution();

    List<ExamResult> publishedResultsFor(Long studentId);
}
