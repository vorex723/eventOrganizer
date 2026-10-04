package com.mazurek.eventOrganizer.jwt;

import com.mazurek.eventOrganizer.user.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
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

    @Size(max = 64)
    @NotBlank
    @Column(nullable = false, unique = true, length = 64)
    private String tokenHash;

    @NotNull
    @Column(nullable = false)
    private UUID familyId = UUID.randomUUID();

    @NotNull
    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotNull
    @Column(nullable = false)
    private Instant expiryDate;

    @Column(nullable = false)
    private boolean revoked = false;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeviceType deviceType;

    @NotNull
    @Column(nullable = false)
    private Instant createdAt;

    @NotNull
    @Column(nullable = false)
    private Instant lastUsedAt;

    public boolean isExpired(Instant now){
        return !now.isBefore(expiryDate);
    }
}
