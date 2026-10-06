package com.mazurek.eventOrganizer.auth.email;

import com.mazurek.eventOrganizer.testData.builders.AuthEmailSendResultTestBuilder;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

@Component
@Profile("test")
public class TestAuthEmailSender implements AuthEmailSender {

    private final AtomicReference<AuthEmailSendResult> result =
            new AtomicReference<>(new AuthEmailSendResultTestBuilder()
                    .outcome(AuthEmailSendOutcome.SENT)
                    .providerMessageId(null)
                    .errorMessage(null)
                    .build());

    @Override
    public AuthEmailSendResult send(AuthEmailType type, String recipientEmail, String rawToken) {
        return result.get();
    }

    public void configureResult(AuthEmailSendResult sendResult) {
        result.set(Objects.requireNonNull(sendResult));
    }

    public void reset() {
        result.set(new AuthEmailSendResultTestBuilder()
                .outcome(AuthEmailSendOutcome.SENT)
                .providerMessageId(null)
                .errorMessage(null)
                .build());
    }
}
