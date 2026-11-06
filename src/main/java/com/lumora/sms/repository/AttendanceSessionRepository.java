package com.lumora.sms.repository;

import com.lumora.sms.entity.AttendanceSession;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceSessionRepository extends JpaRepository<AttendanceSession, Long> {

    Optional<AttendanceSession> findByClassStreamIdAndAttendanceDate(Long classStreamId, LocalDate attendanceDate);

    List<AttendanceSession> findTop20ByOrderByAttendanceDateDesc();

    List<AttendanceSession> findByAttendanceDate(LocalDate attendanceDate);
}
