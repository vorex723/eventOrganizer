package com.mazurek.eventOrganizer.notification.repository;

import com.mazurek.eventOrganizer.notification.domain.NotificationDelivery;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;

@Repository
@RequiredArgsConstructor
public class NotificationDeliveryUpsertRepository {

    private static final String INSERT_IF_ABSENT = """
            insert into notification_deliveries (
                id, notification_id, channel, target_key, target_email,
                target_device_id, target_installation_id, status, attempt_count, created_at
            ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            on conflict (notification_id, channel, target_key) do nothing
            """;

    private final JdbcTemplate jdbcTemplate;

    public boolean insertIfAbsent(NotificationDelivery delivery) {
        return jdbcTemplate.update(
                INSERT_IF_ABSENT,
                delivery.getId(),
                delivery.getNotification().getId(),
                delivery.getChannel().name(),
                delivery.getTargetKey(),
                delivery.getTargetEmail(),
                delivery.getTargetDeviceId(),
                delivery.getTargetInstallationId(),
                delivery.getStatus().name(),
                delivery.getAttemptCount(),
                Timestamp.from(delivery.getCreatedAt())
        ) == 1;
    }
}
