package com.lumora.sms.repository;

import com.lumora.sms.entity.AttendanceRecord;
import com.lumora.sms.enums.AttendanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;

public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long> {

    boolean existsByAttendanceSessionIdAndStudentId(Long attendanceSessionId, Long studentId);

    List<AttendanceRecord> findByAttendanceSessionIdOrderByStudentAdmissionNumberAsc(Long attendanceSessionId);

    List<AttendanceRecord> findByStudentIdOrderByIdDesc(Long studentId);

    long countByAttendanceSessionIdAndStatus(Long attendanceSessionId, AttendanceStatus status);

    /** Present-versus-roll percentage for a single day, used on the dashboard. */
    @Query("""
           SELECT COUNT(r) FROM AttendanceRecord r
           WHERE r.attendanceSession.attendanceDate = :date
             AND r.status = com.lumora.sms.enums.AttendanceStatus.PRESENT
           """)
    long countPresentOn(@Param("date") LocalDate date);

    @Query("SELECT COUNT(r) FROM AttendanceRecord r WHERE r.attendanceSession.attendanceDate = :date")
    long countMarkedOn(@Param("date") LocalDate date);

    /** Daily present counts over a window, for the attendance trend chart. */
    @Query("""
           SELECT r.attendanceSession.attendanceDate,
                  SUM(CASE WHEN r.status = com.lumora.sms.enums.AttendanceStatus.PRESENT THEN 1 ELSE 0 END),
                  COUNT(r)
           FROM AttendanceRecord r
           WHERE r.attendanceSession.attendanceDate >= :from
           GROUP BY r.attendanceSession.attendanceDate
           ORDER BY r.attendanceSession.attendanceDate
           """)
    List<Object[]> dailyTrendSince(@Param("from") LocalDate from);

    /** A learner's own attendance summary. */
    @Query("SELECT r.status, COUNT(r) FROM AttendanceRecord r WHERE r.student.id = :studentId GROUP BY r.status")
    List<Object[]> summaryForStudent(@Param("studentId") Long studentId);
}
