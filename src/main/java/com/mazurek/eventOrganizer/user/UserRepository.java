package com.mazurek.eventOrganizer.user;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    Optional<User> findByIgnoreCaseEmail(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select user from User user where user.id = :id")
    Optional<User> findByIdForUpdate(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select user from User user where lower(user.email) = lower(:email)")
    Optional<User> findByIgnoreCaseEmailForUpdate(@Param("email") String email);

    @Modifying
    @Query("""
            update User user
            set user.notificationPreferencesVersion = user.notificationPreferencesVersion + 1
            where user.id = :userId
              and user.notificationPreferencesVersion = :expectedVersion
            """)
    int advanceNotificationPreferencesVersion(
            @Param("userId") UUID userId,
            @Param("expectedVersion") long expectedVersion
    );

}
