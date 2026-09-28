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
                @UniqueConstraint(columnNames = "token_hash")
        })

public class ActivationToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token_hash", unique = true, nullable = false, length = 64)
    private String tokenHash;

    @Transient
    private UUID token;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true, updatable = false)
    private User user;

    @Column(nullable = false)
    private Instant expirationDate;

    public boolean isExpired(Instant now){
        return !now.isBefore(expirationDate);
    }

    public boolean matches(UUID rawToken) {
        return tokenHash.equals(AuthTokenHash.sha256(rawToken));
    }

    public void issue(UUID rawToken, long expirationMillis, Instant now) {
        this.token = rawToken;
        this.tokenHash = AuthTokenHash.sha256(rawToken);
        this.expirationDate = now.plusMillis(expirationMillis);
    }

    public void regenerate(long expirationMillis, Instant now) {
        issue(UUID.randomUUID(), expirationMillis, now);
    }

}
