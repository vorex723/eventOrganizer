package com.mazurek.eventOrganizer.auth.email;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
@RequiredArgsConstructor
public class LocalAuthEmailSender implements AuthEmailSender {

    private final LocalAuthEmailSink sink;

    @Override
    public AuthEmailSendResult send(AuthEmailType type, String recipientEmail, String rawToken) {
        LocalAuthEmail delivery = sink.record(type, recipientEmail, rawToken);
        return AuthEmailSendResult.sent("local-" + delivery.sentAt().toEpochMilli());
    }
}
