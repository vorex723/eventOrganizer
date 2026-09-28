package com.mazurek.eventOrganizer.auth.email;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

@Component
@Profile({"local", "test"})
public class AuthEmailSenderTestImpl implements AuthEmailSender {

    private final AtomicReference<AuthEmailSendResult> result =
            new AtomicReference<>(AuthEmailSendResult.sent(null));

    @Override
    public AuthEmailSendResult send(AuthEmailType type, String recipientEmail, String rawToken) {
        return result.get();
    }

    public void configureResult(AuthEmailSendResult sendResult) {
        result.set(Objects.requireNonNull(sendResult));
    }

    public void reset() {
        result.set(AuthEmailSendResult.sent(null));
    }
}
