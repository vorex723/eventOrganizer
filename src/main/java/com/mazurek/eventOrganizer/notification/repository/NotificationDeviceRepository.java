package com.mazurek.eventOrganizer.notification.repository;

import com.mazurek.eventOrganizer.notification.domain.DevicePlatform;
import com.mazurek.eventOrganizer.notification.domain.NotificationDevice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationDeviceRepository extends JpaRepository<NotificationDevice, UUID> {

    List<NotificationDevice> findByUserIdAndPlatform(UUID userId, DevicePlatform devicePlatform);
    Optional<NotificationDevice> findByFirebaseInstallationId(String firebaseInstallationId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from NotificationDevice device where device.id = :deviceId")
    int deleteIfExistsById(@Param("deviceId") UUID deviceId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            delete from NotificationDevice device
            where device.id = :deviceId
                and device.userId = :userId
            """)
    int deleteIfOwnedByIdAndUserId(
            @Param("deviceId") UUID deviceId,
            @Param("userId") UUID userId
    );
}
