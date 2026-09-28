package com.mazurek.eventOrganizer.auth.email;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * A development-only inbox. It is deliberately not created in production or
 * test profiles, because its entries contain one-time authentication links.
 */
@RestController
@Profile("local")
@RequiredArgsConstructor
@RequestMapping("/api/v1/dev/auth-emails")
public class LocalAuthEmailController {

    private final LocalAuthEmailSink sink;

    @GetMapping
    public ResponseEntity<List<LocalAuthEmail>> recent() {
        return ResponseEntity.ok(sink.recent());
    }
}
