package com.mazurek.eventOrganizer.auth;

import com.mazurek.eventOrganizer.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "email_change_tokens")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EmailChangeToken {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Transient
    private UUID token;

    @Column(name = "pending_email", nullable = false, unique = true, length = 255)
    private String pendingEmail;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false)
    private Instant expirationDate;

    public boolean isExpired(Instant now) { return !now.isBefore(expirationDate); }

    public void issue(UUID rawToken, String email, long expirationMillis, Instant now) {
        token = rawToken;
        tokenHash = AuthTokenHash.sha256(rawToken);
        pendingEmail = email;
        expirationDate = now.plusMillis(expirationMillis);
    }
}
