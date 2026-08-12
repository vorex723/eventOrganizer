package com.mazurek.eventOrganizer.notification.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID recipientId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 3000)
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationResourceType resourceType;

    @Column(nullable = false)
    private UUID resourceId;

    @Enumerated(EnumType.STRING)
    private NotificationResourceType parentResourceType;

    private UUID parentResourceId;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant readAt;

    @Builder.Default
    @OneToMany(mappedBy = "notification", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<NotificationDelivery> deliveries = new HashSet<>();

    public boolean isRead() {
        return readAt != null;
    }

    public void markAsRead(Instant readAt) {
        if (this.readAt != null) {
            return;
        }
        this.readAt = readAt;
    }

    public void addDelivery(NotificationDelivery delivery) {
        deliveries.add(delivery);
        delivery.setNotification(this);
    }
}
