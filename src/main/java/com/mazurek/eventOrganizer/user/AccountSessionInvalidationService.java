package com.mazurek.eventOrganizer.user;

import com.mazurek.eventOrganizer.jwt.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Invalidates every credential issued before a sensitive account-state change. */
@Service
@RequiredArgsConstructor
public class AccountSessionInvalidationService {

    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public void invalidateAll(User user) {
        user.setSecurityVersion(user.getSecurityVersion() + 1);
        userRepository.save(user);
        refreshTokenService.revokeAllUserTokens(user.getId());
    }
}
