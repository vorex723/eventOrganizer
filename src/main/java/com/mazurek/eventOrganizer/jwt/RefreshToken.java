package com.mazurek.eventOrganizer.jwt;

import com.mazurek.eventOrganizer.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "refresh_tokens")
public class RefreshToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(nullable = false)
    private UUID familyId = UUID.randomUUID();

    /**
     * The raw bearer credential is never mapped to the database. It is retained only
     * on instances created for an HTTP response or test fixture.
     */
    @Transient
    private String token;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
        this.tokenHash = RefreshTokenHash.sha256(token);
    }

    public void setRawToken(String token) {
        this.token = token;
    }

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Instant expiryDate;

    @Column(nullable = false)
    private boolean revoked = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeviceType deviceType;

    @Column
    private String deviceInfo;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant lastUsedAt;

    public boolean isExpired(Instant now){
        return now.isAfter(expiryDate);
    }
}
