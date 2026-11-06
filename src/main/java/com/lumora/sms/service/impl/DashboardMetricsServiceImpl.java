package com.lumora.sms.service.impl;

import com.lumora.sms.dto.*;
import com.lumora.sms.entity.AuditLog;
import com.lumora.sms.enums.LoanStatus;
import com.lumora.sms.repository.*;
import com.lumora.sms.service.AttendanceService;
import com.lumora.sms.service.DashboardMetricsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Read-only aggregation for the dashboards. All figures are whole-school; the
 * per-learner views stay behind DashboardAccessService ownership checks.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardMetricsServiceImpl implements DashboardMetricsService {

    private final StudentRepository students;
    private final TeacherRepository teachers;
    private final StudentInvoiceRepository invoices;
    private final PaymentRepository payments;
    private final BookLoanRepository loans;
    private final ExaminationRepository examinations;
    private final ExamResultRepository results;
    private final StudentEnrollmentRepository enrollments;
    private final AttendanceRecordRepository attendanceRecords;
    private final AuditLogRepository auditLogs;
    private final AttendanceService attendance;

    @Override
    public DashboardSummary summary() {
        LocalDate today = LocalDate.now();
        long marked = attendanceRecords.countMarkedOn(today);
        int attendanceRate = marked == 0
                ? -1
                : (int) Math.round(attendanceRecords.countPresentOn(today) * 100.0 / marked);

        BigDecimal billed = invoices.totalBilled();
        BigDecimal collected = payments.totalCollected();

        return new DashboardSummary(
                students.countByActiveTrue(),
                teachers.count(),
                invoices.count(),
                loans.countByStatus(LoanStatus.BORROWED),
                loans.findOverdue(today).size(),
                examinations.countByPublishedFalse(),
                attendanceRate,
                billed,
                collected,
                invoices.totalOutstanding());
    }

    @Override
    public List<ChartPoint> enrolmentByClass() {
        return enrollments.countPerStream().stream()
                .map(row -> ChartPoint.of(row[0] + " " + row[1], (Number) row[2]))
                .toList();
    }

    @Override
    public List<ChartPoint> subjectPerformance() {
        return results.averageBySubjectOverall().stream()
                .map(row -> ChartPoint.of((String) row[0], (Number) row[1]))
                .toList();
    }

    @Override
    public List<ChartPoint> feeCollectionByMethod() {
        return payments.totalsByMethod().stream()
                .map(row -> ChartPoint.of(String.valueOf(row[0]), (Number) row[1]))
                .toList();
    }

    @Override
    public List<ChartPoint> attendanceTrend(int days) {
        return attendance.attendanceTrend(days);
    }

    @Override
    public List<AuditLog> recentActivity() {
        return auditLogs.findTop15ByOrderByOccurredAtDesc();
    }
}
