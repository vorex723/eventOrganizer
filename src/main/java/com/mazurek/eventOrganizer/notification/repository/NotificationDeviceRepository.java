package com.mazurek.eventOrganizer.notification.repository;

import com.mazurek.eventOrganizer.notification.domain.DevicePlatform;
import com.mazurek.eventOrganizer.notification.domain.NotificationDevice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationDeviceRepository extends JpaRepository<NotificationDevice, UUID> {

    List<NotificationDevice> findByUserIdAndPlatform(UUID userId, DevicePlatform devicePlatform);
    List<NotificationDevice> findByUserIdAndPlatformIn(UUID userId, Collection<DevicePlatform> platforms);
    Optional<NotificationDevice> findByFirebaseInstallationId(String firebaseInstallationId);


    @Query(
            """
            select device.firebaseInstallationId
            from NotificationDevice device
            where device.userId = :userId
              and device.platform in :platforms
            """
    )
    List<String> findAllFirebaseInstallationIdsByUserIdAndPlatformIn(
            @Param("userId") UUID userId,
            @Param("platforms") Collection<DevicePlatform> platforms
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("""
            delete from NotificationDevice device
            where device.firebaseInstallationId in :firebaseInstallationIds
            """)
    int deleteAllByFirebaseInstallationIdIn(
            @Param("firebaseInstallationIds") Collection<String> firebaseInstallationIds
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            delete from NotificationDevice device
            where device.lastSeenAt is null
               or device.lastSeenAt < :staleBefore
            """)
    int deleteAllStaleBefore(@Param("staleBefore") Instant staleBefore);

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

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            delete from NotificationDevice device
            where device.userId = :userId
              and device.firebaseInstallationId = :firebaseInstallationId
            """)
    int deleteByUserIdAndFirebaseInstallationId(
            @Param("userId") UUID userId,
            @Param("firebaseInstallationId") String firebaseInstallationId
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            delete from NotificationDevice device
            where device.id = :deviceId
              and device.userId = :userId
              and device.firebaseInstallationId = :firebaseInstallationId
            """)
    int deleteIfOwnedByIdAndUserIdAndFirebaseInstallationId(
            @Param("deviceId") UUID deviceId,
            @Param("userId") UUID userId,
            @Param("firebaseInstallationId") String firebaseInstallationId
    );
}
