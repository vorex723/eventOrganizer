package com.mazurek.eventOrganizer.notification;

import com.mazurek.eventOrganizer.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name= "notifications")
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne//(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "user_id")
    private User receiver;
    @Builder.Default
    private Boolean opened = false;
    @Builder.Default
    private String title = "";
    @Builder.Default
    private String body = "";
    @Enumerated(EnumType.STRING)
    private NotificationType type;
    private UUID resourceId;
    @Builder.Default
    private LocalDateTime createDate = LocalDateTime.now();
}
