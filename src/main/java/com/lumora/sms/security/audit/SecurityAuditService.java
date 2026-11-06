package com.lumora.sms.security.audit;
import com.lumora.sms.entity.*; import com.lumora.sms.repository.*; import lombok.RequiredArgsConstructor; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.*; import java.time.LocalDateTime;
/** Writes security events to the immutable audit trail without sensitive payloads. */
@Service @RequiredArgsConstructor
public class SecurityAuditService { private final AuditLogRepository auditLogs; private final UserRepository users;
 @Transactional(propagation=Propagation.REQUIRES_NEW) public void record(String username,String action,String ip){ AuditLog log=new AuditLog();if(username!=null)users.findByUsername(username).ifPresent(log::setUser);log.setAction(action);log.setEntityName("SECURITY");log.setIpAddress(ip);log.setOccurredAt(LocalDateTime.now());auditLogs.save(log); }
}
