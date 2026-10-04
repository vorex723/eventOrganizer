package com.mazurek.eventOrganizer.auth;

import com.mazurek.eventOrganizer.user.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "activation_tokens")

public class ActivationToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Size(max = 64)
    @NotBlank
    @Column(name = "token_hash", unique = true, nullable = false, length = 64)
    private String tokenHash;

    @Transient
    private UUID token;

    @NotNull
    @OneToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true, updatable = false)
    private User user;

    @NotNull
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
