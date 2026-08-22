package com.mazurek.eventOrganizer.notification.repository;

import com.mazurek.eventOrganizer.notification.domain.NotificationPreference;
import com.mazurek.eventOrganizer.notification.domain.NotificationResourceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, UUID> {
    List<NotificationPreference> findByUserId(UUID userId);

    List<NotificationPreference> findByUserIdAndResourceType(
            UUID userId,
            NotificationResourceType resourceType
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
       delete from NotificationPreference preference
       where preference.userId = :userId
       """)
    int deleteAllByUserId(@Param("userId") UUID userId);
}
