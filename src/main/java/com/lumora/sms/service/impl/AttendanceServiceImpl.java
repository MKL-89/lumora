package com.lumora.sms.service.impl;

import com.lumora.sms.dto.*;
import com.lumora.sms.entity.*;
import com.lumora.sms.enums.AttendanceStatus;
import com.lumora.sms.exception.*;
import com.lumora.sms.repository.*;
import com.lumora.sms.service.AttendanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Persists one attendance record per learner, per stream, per date. */
@Service
@org.springframework.validation.annotation.Validated
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AttendanceServiceImpl implements AttendanceService {

    private final AttendanceSessionRepository sessions;
    private final AttendanceRecordRepository records;
    private final ClassStreamRepository streams;
    private final TeacherRepository teachers;
    private final StudentRepository students;

    @Override
    @Transactional
    public AttendanceRecord record(@Valid AttendanceEntryRequest r) {
        Student student = students.findById(r.studentId())
                .orElseThrow(() -> new ResourceNotFoundException("Student", r.studentId()));
        AttendanceSession session = openSession(r.classStreamId(), r.teacherId(), r.attendanceDate());

        if (records.existsByAttendanceSessionIdAndStudentId(session.getId(), student.getId())) {
            throw new BusinessRuleViolationException(
                    "Attendance cannot be entered twice for the same student on the same day.");
        }
        AttendanceRecord record = new AttendanceRecord();
        record.setAttendanceSession(session);
        record.setStudent(student);
        record.setStatus(r.status());
        record.setRemarks(r.remarks());
        return records.save(record);
    }

    /** Finds the register for the stream on that date, creating it on first use. */
    private AttendanceSession openSession(Long classStreamId, Long teacherId, LocalDate date) {
        return sessions.findByClassStreamIdAndAttendanceDate(classStreamId, date)
                .orElseGet(() -> {
                    ClassStream stream = streams.findById(classStreamId)
                            .orElseThrow(() -> new ResourceNotFoundException("Class stream", classStreamId));
                    Teacher teacher = teachers.findById(teacherId)
                            .orElseThrow(() -> new ResourceNotFoundException("Teacher", teacherId));
                    AttendanceSession session = new AttendanceSession();
                    session.setClassStream(stream);
                    session.setRecordedBy(teacher);
                    session.setAttendanceDate(date);
                    return sessions.save(session);
                });
    }

    @Override
    public List<RegisterRow> register(Long classStreamId, LocalDate date) {
        Map<Long, AttendanceStatus> existing = sessions
                .findByClassStreamIdAndAttendanceDate(classStreamId, date)
                .map(session -> records.findByAttendanceSessionIdOrderByStudentAdmissionNumberAsc(session.getId())
                        .stream()
                        .collect(Collectors.toMap(r -> r.getStudent().getId(), AttendanceRecord::getStatus,
                                                  (a, b) -> a)))
                .orElseGet(Map::of);

        return students.findEnrolledInStream(classStreamId).stream()
                .map(student -> new RegisterRow(
                        student.getId(),
                        student.getAdmissionNumber(),
                        fullName(student),
                        existing.get(student.getId())))
                .toList();
    }

    /**
     * Writes the whole register in one transaction. Unlike the single-entry method,
     * a row that already exists is corrected rather than rejected, because a teacher
     * fixing a mis-click is a normal part of taking a register.
     */
    @Override
    @Transactional
    public int saveRegister(Long classStreamId, Long teacherId, LocalDate date, Map<Long, AttendanceStatus> marks) {
        if (marks == null || marks.isEmpty()) {
            throw new BusinessRuleViolationException("Select a status for at least one learner before saving.");
        }
        if (date.isAfter(LocalDate.now())) {
            throw new BusinessRuleViolationException("Attendance cannot be taken for a future date.");
        }

        AttendanceSession session = openSession(classStreamId, teacherId, date);
        Map<Long, AttendanceRecord> onFile = records
                .findByAttendanceSessionIdOrderByStudentAdmissionNumberAsc(session.getId()).stream()
                .collect(Collectors.toMap(r -> r.getStudent().getId(), Function.identity(), (a, b) -> a));

        Set<Long> enrolled = students.findEnrolledInStream(classStreamId).stream()
                .map(Student::getId)
                .collect(Collectors.toSet());

        int written = 0;
        for (Map.Entry<Long, AttendanceStatus> entry : marks.entrySet()) {
            if (entry.getValue() == null) {
                continue;
            }
            if (!enrolled.contains(entry.getKey())) {
                throw new BusinessRuleViolationException(
                        "A learner outside this stream cannot appear on its register.");
            }
            AttendanceRecord record = onFile.get(entry.getKey());
            if (record == null) {
                record = new AttendanceRecord();
                record.setAttendanceSession(session);
                record.setStudent(students.getReferenceById(entry.getKey()));
            }
            record.setStatus(entry.getValue());
            records.save(record);
            written++;
        }
        return written;
    }

    @Override
    public AttendanceSummaryView summaryFor(Long studentId) {
        Map<AttendanceStatus, Long> counts = new EnumMap<>(AttendanceStatus.class);
        for (Object[] row : records.summaryForStudent(studentId)) {
            counts.put((AttendanceStatus) row[0], ((Number) row[1]).longValue());
        }
        return new AttendanceSummaryView(
                counts.getOrDefault(AttendanceStatus.PRESENT, 0L),
                counts.getOrDefault(AttendanceStatus.ABSENT, 0L),
                counts.getOrDefault(AttendanceStatus.LATE, 0L),
                counts.getOrDefault(AttendanceStatus.EXCUSED, 0L));
    }

    @Override
    public List<ChartPoint> attendanceTrend(int days) {
        LocalDate from = LocalDate.now().minusDays(days);
        List<ChartPoint> points = new ArrayList<>();
        for (Object[] row : records.dailyTrendSince(from)) {
            LocalDate date = (LocalDate) row[0];
            long present = ((Number) row[1]).longValue();
            long total = ((Number) row[2]).longValue();
            int rate = total == 0 ? 0 : (int) Math.round(present * 100.0 / total);
            points.add(ChartPoint.of(date.getDayOfMonth() + "/" + date.getMonthValue(), rate));
        }
        return points;
    }

    private String fullName(Student s) {
        return s.getMiddleName() == null || s.getMiddleName().isBlank()
                ? s.getFirstName() + " " + s.getLastName()
                : s.getFirstName() + " " + s.getMiddleName() + " " + s.getLastName();
    }
}
