package com.lumora.sms.repository;

import com.lumora.sms.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findTop15ByOrderByOccurredAtDesc();

    List<AuditLog> findTop100ByOrderByOccurredAtDesc();
}
