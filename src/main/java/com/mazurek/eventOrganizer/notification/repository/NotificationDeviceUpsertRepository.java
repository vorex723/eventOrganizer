package com.mazurek.eventOrganizer.notification.repository;

import com.mazurek.eventOrganizer.notification.domain.DevicePlatform;
import com.mazurek.eventOrganizer.notification.domain.NotificationDevice;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class NotificationDeviceUpsertRepository {

    private static final String UPSERT_DEVICE = """
            insert into notification_devices (
                id,
                user_id,
                platform,
                firebase_installation_id,
                created_at,
                last_seen_at
            )
            values (?, ?, ?, ?, ?, ?)
            on conflict (firebase_installation_id) do update
            set user_id = excluded.user_id,
                last_seen_at = excluded.last_seen_at
            returning id, user_id, platform, firebase_installation_id, created_at, last_seen_at
            """;

    private final JdbcTemplate jdbcTemplate;

    public NotificationDevice upsert(
            UUID userId,
            DevicePlatform platform,
            String firebaseInstallationId,
            Instant now
    ) {
        return jdbcTemplate.queryForObject(
                UPSERT_DEVICE,
                (resultSet, rowNumber) -> NotificationDevice.builder()
                        .id(resultSet.getObject("id", UUID.class))
                        .userId(resultSet.getObject("user_id", UUID.class))
                        .platform(DevicePlatform.valueOf(resultSet.getString("platform")))
                        .firebaseInstallationId(resultSet.getString("firebase_installation_id"))
                        .createdAt(resultSet.getTimestamp("created_at").toInstant())
                        .lastSeenAt(resultSet.getTimestamp("last_seen_at").toInstant())
                        .build(),
                UUID.randomUUID(),
                userId,
                platform.name(),
                firebaseInstallationId,
                Timestamp.from(now),
                Timestamp.from(now)
        );
    }
}
