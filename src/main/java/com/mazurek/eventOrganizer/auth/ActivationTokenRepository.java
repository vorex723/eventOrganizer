package com.mazurek.eventOrganizer.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ActivationTokenRepository extends JpaRepository<ActivationToken, Long> {
    Optional<ActivationToken> findByIgnoreCaseUserEmail(String email);
    Optional<ActivationToken> findByToken(UUID token);

}
