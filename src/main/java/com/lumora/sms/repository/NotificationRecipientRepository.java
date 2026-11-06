package com.lumora.sms.repository;

import com.lumora.sms.entity.NotificationRecipient;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NotificationRecipientRepository extends JpaRepository<NotificationRecipient, Long> {

    List<NotificationRecipient> findByUserIdOrderByIdDesc(Long userId);

    long countByUserIdAndReadAtIsNull(Long userId);
}
