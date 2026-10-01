package com.mazurek.eventOrganizer.auth;

import com.mazurek.eventOrganizer.auth.email.AuthEmailType;
import com.mazurek.eventOrganizer.exception.auth.EmailChangeAddressUnavailableException;
import com.mazurek.eventOrganizer.exception.user.UserNotFoundException;
import com.mazurek.eventOrganizer.config.properties.AuthProperties;
import com.mazurek.eventOrganizer.notification.service.EmailService;
import com.mazurek.eventOrganizer.user.AccountSessionInvalidationService;
import com.mazurek.eventOrganizer.user.User;
import com.mazurek.eventOrganizer.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmailChangeService {
    private final EmailChangeTokenRepository emailChangeTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final UserRepository userRepository;
    private final AccountSessionInvalidationService accountSessionInvalidationService;
    private final EmailService emailService;
    private final AuthProperties authProperties;
    private final Clock clock;
    private final AuthUserLockService authUserLockService;

    @Transactional
    public void requestChange(User candidate, String requestedEmail) {
        User user = authUserLockService.lockById(candidate.getId()).orElseThrow(UserNotFoundException::new);
        String normalizedEmail = requestedEmail.toLowerCase(Locale.ROOT);
        Instant now = clock.instant();
        EmailChangeToken token = emailChangeTokenRepository.findByUserIdForUpdate(user.getId())
                .orElseGet(() -> EmailChangeToken.builder().user(user).build());
        if (normalizedEmail.equals(token.getPendingEmail()) && !token.isExpired(now)
                && emailService.wasRecentlyRequested(user.getId(), AuthEmailType.EMAIL_CHANGE_CONFIRMATION)) return;

        emailService.cancelPendingEmails(user.getId(), AuthEmailType.EMAIL_CHANGE_CONFIRMATION);
        UUID rawToken = UUID.randomUUID();
        token.issue(rawToken, normalizedEmail, authProperties.getEmailChangeTokenExpiration(), now);
        try {
            emailChangeTokenRepository.saveAndFlush(token);
        } catch (DataIntegrityViolationException exception) {
            if (!EmailUniqueConstraint.isPendingEmailConflict(exception)) {
                throw exception;
            }
            throw new EmailChangeAddressUnavailableException(exception);
        }
        emailService.sendEmailChangeConfirmationEmail(user.getId(), normalizedEmail, rawToken);
    }

    @Transactional
    public EmailChangeResult confirmChange(UUID rawToken) {
        String tokenHash = AuthTokenHash.sha256(rawToken);
        UUID userId = emailChangeTokenRepository.findUserIdByTokenHash(tokenHash).orElse(null);
        if (userId == null) return EmailChangeResult.INVALID_TOKEN;
        User user = authUserLockService.lockById(userId).orElse(null);
        if (user == null) return EmailChangeResult.INVALID_TOKEN;
        // Cleanup locks password resets before email changes. Keep the same order.
        PasswordResetToken resetToken = passwordResetTokenRepository.findByUserIdForUpdate(userId).orElse(null);
        EmailChangeToken token = emailChangeTokenRepository.findByTokenHash(tokenHash).orElse(null);
        if (token == null) return EmailChangeResult.INVALID_TOKEN;
        if (token.isExpired(clock.instant())) {
            emailChangeTokenRepository.delete(token);
            emailService.cancelPendingEmails(user.getId(), AuthEmailType.EMAIL_CHANGE_CONFIRMATION);
            return EmailChangeResult.EXPIRED;
        }
        if (userRepository.findByIgnoreCaseEmail(token.getPendingEmail())
                .filter(existing -> !existing.getId().equals(user.getId())).isPresent()) return EmailChangeResult.EMAIL_UNAVAILABLE;

        user.setEmail(token.getPendingEmail());
        user.setLastCredentialsChangeTime(clock.instant());
        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            if (!EmailUniqueConstraint.isUserEmailConflict(exception)) {
                throw exception;
            }
            throw new EmailChangeAddressUnavailableException(exception);
        }
        if (resetToken != null) passwordResetTokenRepository.delete(resetToken);
        emailChangeTokenRepository.delete(token);
        emailService.cancelPendingEmails(user.getId(), AuthEmailType.EMAIL_CHANGE_CONFIRMATION);
        emailService.cancelPendingEmails(user.getId(), AuthEmailType.PASSWORD_RESET);
        accountSessionInvalidationService.invalidateAll(user);
        return EmailChangeResult.CHANGED;
    }
}
