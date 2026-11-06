package com.lumora.sms.repository;

import com.lumora.sms.entity.Announcement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

    List<Announcement> findAllByOrderByPublishedAtDesc();

    /** Announcements that have gone live and have not yet expired. */
    @Query("""
           SELECT a FROM Announcement a
           WHERE a.publishedAt <= :now AND (a.expiresAt IS NULL OR a.expiresAt > :now)
           ORDER BY a.publishedAt DESC
           """)
    List<Announcement> findCurrent(@Param("now") LocalDateTime now);
}
