package com.mazurek.eventOrganizer.auth;

import com.mazurek.eventOrganizer.user.User;
import jakarta.persistence.Column;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "password_reset_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Size(max = 64)
    @NotBlank
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @NotNull
    @OneToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @NotNull
    @Column(nullable = false)
    private Instant expirationDate;

    public boolean isExpired(Instant now) {
        return !now.isBefore(expirationDate);
    }

    public void issue(UUID rawToken, long expirationMillis, Instant now) {
        tokenHash = AuthTokenHash.sha256(rawToken);
        expirationDate = now.plusMillis(expirationMillis);
    }
}
