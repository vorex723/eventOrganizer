package com.mazurek.eventOrganizer.auth;

import com.mazurek.eventOrganizer.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "activation_tokens",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "user_id"),
                @UniqueConstraint(columnNames = "token")
        })

public class ActivationToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, updatable = true)
    private UUID token;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true, updatable = false)
    private User user;

    @Column(nullable = false)
    private Instant expirationDate;

    public boolean isExpired(){
        return Instant.now().isAfter(expirationDate);
    }
    public void regenerate(long expirationSeconds) {
        this.token = UUID.randomUUID();
        this.expirationDate = Instant.now().plusMillis(expirationSeconds);
    }

}
