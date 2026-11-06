package com.lumora.sms.service;

import com.lumora.sms.dto.AnnouncementRequest;
import com.lumora.sms.entity.*;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Set;

/** Publishes announcements and delivers notifications to explicit recipients. */
public interface NotificationService {

    Announcement publish(Announcement announcement);

    /** Publishes a notice authored by the signed-in user. */
    Announcement publish(@Valid AnnouncementRequest request, String authorUsername);

    Notification notify(Notification notification, Set<Long> recipientUserIds);

    List<Announcement> announcements();

    List<Announcement> currentAnnouncements();

    List<Notification> notifications();

    List<NotificationRecipient> inboxFor(String username);
}
