package com.mazurek.eventOrganizer.notification.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notification_devices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationDevice {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DevicePlatform platform;

    @Column(nullable = false, unique = true, length = 255)
    private String firebaseInstallationId;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant lastSeenAt;

}
