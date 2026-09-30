package com.mazurek.eventOrganizer.auth;

import com.mazurek.eventOrganizer.user.User;
import com.mazurek.eventOrganizer.user.UserRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/** Serializes account credential changes before any token or outbox writes. */
@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class AuthUserLockService {
    private final UserRepository userRepository;
    private final EntityManager entityManager;

    public Optional<User> lockById(UUID userId) {
        return userRepository.findByIdForUpdate(userId).map(this::refreshLocked);
    }

    public Optional<User> lockByEmail(String email) {
        return userRepository.findByIgnoreCaseEmailForUpdate(email).map(this::refreshLocked)
                .filter(user -> user.getEmail().equalsIgnoreCase(email));
    }

    private User refreshLocked(User user) {
        // A locking query can return an already managed, stale entity after waiting.
        // Refresh only before making changes; the row lock is already held until commit.
        entityManager.refresh(user);
        return user;
    }
}
