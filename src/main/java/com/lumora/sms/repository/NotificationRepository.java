package com.lumora.sms.repository;

import com.lumora.sms.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findAllByOrderBySentAtDesc();
}
