package com.mazurek.eventOrganizer.auth;

import com.mazurek.eventOrganizer.user.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "email_change_tokens")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EmailChangeToken {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Size(max = 64)
    @NotBlank
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Transient
    private UUID token;

    @Size(max = 255)
    @NotBlank
    @Column(name = "pending_email", nullable = false, unique = true, length = 255)
    private String pendingEmail;

    @NotNull
    @OneToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @NotNull
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
