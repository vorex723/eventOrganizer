package com.mazurek.eventOrganizer.config;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.mazurek.eventOrganizer.auth.email.AuthEmailType;
import com.mazurek.eventOrganizer.auth.email.SmtpAuthEmailSender;
import com.mazurek.eventOrganizer.jwt.JwtRequestFilter;
import com.mazurek.eventOrganizer.jwt.JwtUtils;
import com.mazurek.eventOrganizer.notification.delivery.SmtpNotificationEmailClient;
import com.mazurek.eventOrganizer.notification.domain.Notification;
import com.mazurek.eventOrganizer.notification.domain.NotificationResourceType;
import com.mazurek.eventOrganizer.notification.firebaseCloudMessaging.FcmApiClientProdImpl;
import com.mazurek.eventOrganizer.testData.builders.NotificationTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.MailPropertiesTestBuilder;
import com.mazurek.eventOrganizer.user.UserRepository;
import com.mazurek.eventOrganizer.utils.NotificationResourceLinkResolver;
import com.mazurek.eventOrganizer.utils.DeviceTypeResolver;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("SensitiveLoggingUnitTest contracts:")
class SensitiveLoggingUnitTest {
    private static final String SECRET = "secret-that-must-not-appear-in-logs";

    @Test
    void whenDeviceHeadersAreInvalidShouldNotLogRawValues() throws Exception {
        var resolver = new DeviceTypeResolver();
        assertSanitizedLogging(DeviceTypeResolver.class, () -> resolver.determineDeviceType(SECRET, SECRET));
    }

    @Test
    void whenJwtIsRejectedShouldNotLogTokenOrExceptionPayload() throws Exception {
        JwtUtils tokens = mock(JwtUtils.class);
        when(tokens.isTokenValid(SECRET)).thenThrow(new IllegalArgumentException("Rejected token " + SECRET));
        var filter = new JwtRequestFilter(tokens, mock(UserRepository.class));
        var request = new MockHttpServletRequest("GET", "/api/v1/users/me");
        request.addHeader("Authorization", "Bearer " + SECRET);
        try {
            assertSanitizedLogging(JwtRequestFilter.class, () ->
                    filter.doFilter(request, new MockHttpServletResponse(), mock(FilterChain.class)));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void whenAuthEmailFailsShouldNotLogTokenOrProviderPayload() throws Exception {
        JavaMailSender mail = mock(JavaMailSender.class);
        when(mail.createMimeMessage()).thenThrow(new MailSendException("Provider echoed " + SECRET));
        var sender = new SmtpAuthEmailSender(mail, new MailPropertiesTestBuilder()
                .activationBaseUrl(null)
                .passwordResetBaseUrl(null)
                .emailChangeBaseUrl(null)
                .fromAddress(null)
                .build());

        assertSanitizedLogging(SmtpAuthEmailSender.class, () ->
                sender.send(AuthEmailType.ACCOUNT_ACTIVATION, "recipient@example.com", SECRET));
    }

    @Test
    void whenNotificationEmailFailsShouldNotLogProviderPayload() throws Exception {
        JavaMailSender mail = mock(JavaMailSender.class);
        when(mail.createMimeMessage()).thenThrow(new MailSendException("Provider echoed " + SECRET));
        var sender = new SmtpNotificationEmailClient(mail, new MailPropertiesTestBuilder()
                .activationBaseUrl(null)
                .passwordResetBaseUrl(null)
                .emailChangeBaseUrl(null)
                .fromAddress(null)
                .build());

        assertSanitizedLogging(SmtpNotificationEmailClient.class, () ->
                sender.send("recipient@example.com", "Title", "Body"));
    }

    @Test
    void whenFcmFailsShouldNotLogTargetOrProviderPayload() throws Exception {
        FirebaseMessaging firebase = mock(FirebaseMessaging.class);
        FirebaseMessagingException failure = mock(FirebaseMessagingException.class);
        when(failure.getMessagingErrorCode()).thenReturn(MessagingErrorCode.INTERNAL);
        when(failure.getMessage()).thenReturn("Provider echoed " + SECRET);
        when(firebase.sendEach(anyList())).thenThrow(failure);
        var client = new FcmApiClientProdImpl(firebase, mock(NotificationResourceLinkResolver.class));
        var notification = new NotificationTestBuilder()
                .id(UUID.randomUUID())
                .resourceType(NotificationResourceType.EVENT)
                .resourceId(UUID.randomUUID())
                .recipientId(null)
                .createdAt(null)
                .build();

        assertSanitizedLogging(FcmApiClientProdImpl.class, () ->
                client.sendNotificationToInstallationMobile(notification, SECRET));
    }

    private void assertSanitizedLogging(Class<?> type, CheckedAction action) throws Exception {
        Logger logger = (Logger) LoggerFactory.getLogger(type);
        Level originalLevel = logger.getLevel();
        var appender = new ListAppender<ILoggingEvent>();
        appender.setContext(logger.getLoggerContext());
        appender.start();
        logger.addAppender(appender);
        logger.setLevel(Level.DEBUG);
        SecurityContextHolder.clearContext();
        try {
            action.run();
            assertThat(appender.list).isNotEmpty().allSatisfy(event -> {
                assertThat(event.getFormattedMessage()).doesNotContain(SECRET);
                assertThat(event.getThrowableProxy()).isNull();
            });
        } finally {
            logger.detachAppender(appender);
            logger.setLevel(originalLevel);
            appender.stop();
            SecurityContextHolder.clearContext();
        }
    }

    @FunctionalInterface
    private interface CheckedAction {
        void run() throws Exception;
    }
}
