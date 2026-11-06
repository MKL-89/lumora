package com.lumora.sms.service.impl;

import com.lumora.sms.dto.AnnouncementRequest;
import com.lumora.sms.entity.*;
import com.lumora.sms.exception.ResourceNotFoundException;
import com.lumora.sms.repository.*;
import com.lumora.sms.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/** Delivers announcements and notifications without exposing recipient lists to callers. */
@Service
@org.springframework.validation.annotation.Validated
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationServiceImpl implements NotificationService {

    private final AnnouncementRepository announcements;
    private final NotificationRepository notifications;
    private final NotificationRecipientRepository recipients;
    private final UserRepository users;

    @Override
    @Transactional
    public Announcement publish(Announcement announcement) {
        return announcements.save(announcement);
    }

    @Override
    @Transactional
    public Announcement publish(@Valid AnnouncementRequest request, String authorUsername) {
        User author = users.findByUsername(authorUsername)
                .orElseThrow(() -> new ResourceNotFoundException("User", authorUsername));

        LocalDateTime now = LocalDateTime.now();
        Announcement announcement = new Announcement();
        announcement.setCreatedBy(author);
        announcement.setTitle(request.title());
        announcement.setContent(request.content());
        announcement.setPublishedAt(now);
        if (request.daysVisible() != null && request.daysVisible() > 0) {
            announcement.setExpiresAt(now.plusDays(request.daysVisible()));
        }
        return announcements.save(announcement);
    }

    @Override
    @Transactional
    public Notification notify(Notification notification, Set<Long> recipientIds) {
        Notification saved = notifications.save(notification);
        for (Long id : recipientIds) {
            User user = users.findById(id).orElseThrow(() -> new ResourceNotFoundException("User", id));
            NotificationRecipient recipient = new NotificationRecipient();
            recipient.setNotification(saved);
            recipient.setUser(user);
            recipients.save(recipient);
        }
        return saved;
    }

    @Override
    public List<Announcement> announcements() {
        return announcements.findAllByOrderByPublishedAtDesc();
    }

    @Override
    public List<Announcement> currentAnnouncements() {
        return announcements.findCurrent(LocalDateTime.now());
    }

    @Override
    public List<Notification> notifications() {
        return notifications.findAllByOrderBySentAtDesc();
    }

    @Override
    public List<NotificationRecipient> inboxFor(String username) {
        return users.findByUsername(username)
                .map(user -> recipients.findByUserIdOrderByIdDesc(user.getId()))
                .orElseGet(List::of);
    }
}
