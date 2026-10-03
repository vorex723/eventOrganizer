package com.mazurek.eventOrganizer.notification.firebaseCloudMessaging;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.SendResponse;
import com.mazurek.eventOrganizer.notification.domain.Notification;
import com.mazurek.eventOrganizer.notification.domain.NotificationChannel;
import com.mazurek.eventOrganizer.notification.domain.NotificationResourceType;
import com.mazurek.eventOrganizer.utils.NotificationResourceLinkResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static com.mazurek.eventOrganizer.notification.domain.NotificationChannel.PUSH_MOBILE;
import static com.mazurek.eventOrganizer.notification.domain.NotificationChannel.PUSH_WEB;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FcmApiClientProdImplUnitTest {

    private static final String INSTALLATION_ID = "target-installation";

    @Mock
    private FirebaseMessaging firebaseMessaging;
    @Mock
    private NotificationResourceLinkResolver notificationResourceLinkResolver;
    @Mock
    private BatchResponse batchResponse;

    private FcmApiClient client;

    @BeforeEach
    void setUp() {
        client = new FcmApiClientProdImpl(
                firebaseMessaging,
                notificationResourceLinkResolver
        );
    }

    @Test
    void mobileDeliverySendsOnlyToTheGivenInstallationWithDirectResourcePayload() throws Exception {
        Notification notification = notification(NotificationResourceType.EVENT);
        givenResponses(successfulResponse());

        FcmSendResult result = client.sendNotificationToInstallationMobile(notification, INSTALLATION_ID);

        assertSuccessfulSingleTarget(result);
        Message message = sentMessage();
        assertThat(ReflectionTestUtils.getField(message, "fid")).isEqualTo(INSTALLATION_ID);
        assertNotification(message, notification);
        assertThat(messageData(message)).containsExactlyInAnyOrderEntriesOf(Map.of(
                "notificationId", notification.getId().toString(),
                "resourceType", NotificationResourceType.EVENT.toString(),
                "resourceId", notification.getResourceId().toString()
        ));
        verifyNoInteractions(notificationResourceLinkResolver);
    }

    @Test
    void mobileDeliveryIncludesParentResourceDataWhenPresent() throws Exception {
        Notification notification = notification(NotificationResourceType.THREAD);
        UUID eventId = UUID.randomUUID();
        notification.setParentResourceType(NotificationResourceType.EVENT);
        notification.setParentResourceId(eventId);
        givenResponses(successfulResponse());

        FcmSendResult result = client.sendNotificationToInstallationMobile(notification, INSTALLATION_ID);

        assertSuccessfulSingleTarget(result);
        assertThat(messageData(sentMessage())).containsExactlyInAnyOrderEntriesOf(Map.of(
                "notificationId", notification.getId().toString(),
                "resourceType", NotificationResourceType.THREAD.toString(),
                "resourceId", notification.getResourceId().toString(),
                "parentResourceType", NotificationResourceType.EVENT.toString(),
                "parentResourceId", eventId.toString()
        ));
        verifyNoInteractions(notificationResourceLinkResolver);
    }

    @Test
    void webDeliverySendsOnlyToTheGivenInstallationWithLinkAndNotificationId() throws Exception {
        Notification notification = notification(NotificationResourceType.EVENT);
        String link = "https://example.com/events/" + notification.getResourceId();
        when(notificationResourceLinkResolver.resolve(notification)).thenReturn(link);
        givenResponses(successfulResponse());

        FcmSendResult result = client.sendNotificationToInstallationWeb(notification, INSTALLATION_ID);

        assertSuccessfulSingleTarget(result);
        Message message = sentMessage();
        assertThat(ReflectionTestUtils.getField(message, "fid")).isEqualTo(INSTALLATION_ID);
        assertNotification(message, notification);
        assertThat(messageData(message)).containsExactly(Map.entry("notificationId", notification.getId().toString()));
        Object webpushConfig = ReflectionTestUtils.getField(message, "webpushConfig");
        Object fcmOptions = ReflectionTestUtils.getField(webpushConfig, "fcmOptions");
        assertThat(ReflectionTestUtils.getField(fcmOptions, "link")).isEqualTo(link);
    }

    @ParameterizedTest
    @MethodSource("perTargetFailures")
    void classifiesSingleInstallationResponseWithoutDeletingDevice(
            NotificationChannel channel,
            FailureCase failure
    ) throws Exception {
        Notification notification = notification(NotificationResourceType.EVENT);
        stubWebLinkIfNeeded(channel, notification);
        givenResponses(failedResponse(failure.errorCode()));

        FcmSendResult result = send(channel, notification);

        assertFailure(result, failure);
        assertThat(ReflectionTestUtils.getField(sentMessage(), "fid")).isEqualTo(INSTALLATION_ID);
    }

    @ParameterizedTest
    @EnumSource(value = NotificationChannel.class, names = {"PUSH_MOBILE", "PUSH_WEB"})
    void missingProviderErrorCodeIsRetryable(NotificationChannel channel) throws Exception {
        Notification notification = notification(NotificationResourceType.EVENT);
        stubWebLinkIfNeeded(channel, notification);
        givenResponses(failedResponse(null));

        FcmSendResult result = send(channel, notification);

        assertThat(result.outcome()).isEqualTo(FcmSendOutcome.RETRYABLE_FAILURE);
        assertThat(result.targetCount()).isOne();
        assertThat(result.retryableFailureCount()).isOne();
        assertThat(result.errorMessage()).contains("UNKNOWN");
    }

    @ParameterizedTest
    @MethodSource("topLevelFailures")
    void classifiesTopLevelProviderFailureForOnlyTheTargetedInstallation(
            NotificationChannel channel,
            FailureCase failure
    ) throws Exception {
        Notification notification = notification(NotificationResourceType.EVENT);
        stubWebLinkIfNeeded(channel, notification);
        FirebaseMessagingException exception = mock(FirebaseMessagingException.class);
        when(exception.getMessagingErrorCode()).thenReturn(failure.errorCode());
        when(firebaseMessaging.sendEach(anyList())).thenThrow(exception);

        FcmSendResult result = send(channel, notification);

        assertFailure(result, failure);
        assertThat(ReflectionTestUtils.getField(sentMessage(), "fid")).isEqualTo(INSTALLATION_ID);
    }

    @ParameterizedTest
    @EnumSource(value = NotificationChannel.class, names = {"PUSH_MOBILE", "PUSH_WEB"})
    void topLevelProviderFailureWithoutErrorCodeIsRetryable(NotificationChannel channel) throws Exception {
        Notification notification = notification(NotificationResourceType.EVENT);
        stubWebLinkIfNeeded(channel, notification);
        FirebaseMessagingException exception = mock(FirebaseMessagingException.class);
        when(firebaseMessaging.sendEach(anyList())).thenThrow(exception);

        FcmSendResult result = send(channel, notification);

        assertThat(result.outcome()).isEqualTo(FcmSendOutcome.RETRYABLE_FAILURE);
        assertThat(result.targetCount()).isOne();
        assertThat(result.retryableFailureCount()).isOne();
        assertThat(result.errorMessage()).contains("UNKNOWN");
        assertThat(ReflectionTestUtils.getField(sentMessage(), "fid")).isEqualTo(INSTALLATION_ID);
    }

    @ParameterizedTest
    @EnumSource(value = NotificationChannel.class, names = {"PUSH_MOBILE", "PUSH_WEB"})
    void rejectsProviderResponseCountMismatch(NotificationChannel channel) throws Exception {
        Notification notification = notification(NotificationResourceType.EVENT);
        stubWebLinkIfNeeded(channel, notification);
        givenResponses();

        assertThatThrownBy(() -> send(channel, notification))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("FCM returned a different number of responses than submitted messages.");
        assertThat(ReflectionTestUtils.getField(sentMessage(), "fid")).isEqualTo(INSTALLATION_ID);
    }

    private static Stream<Arguments> perTargetFailures() {
        List<FailureCase> cases = List.of(
                new FailureCase(MessagingErrorCode.UNREGISTERED, FcmSendOutcome.NO_TARGETS, 1, 0, 0),
                new FailureCase(MessagingErrorCode.INTERNAL, FcmSendOutcome.RETRYABLE_FAILURE, 0, 1, 0),
                new FailureCase(MessagingErrorCode.QUOTA_EXCEEDED, FcmSendOutcome.RETRYABLE_FAILURE, 0, 1, 0),
                new FailureCase(MessagingErrorCode.UNAVAILABLE, FcmSendOutcome.RETRYABLE_FAILURE, 0, 1, 0),
                new FailureCase(MessagingErrorCode.INVALID_ARGUMENT, FcmSendOutcome.PERMANENT_FAILURE, 0, 0, 1),
                new FailureCase(MessagingErrorCode.SENDER_ID_MISMATCH, FcmSendOutcome.PERMANENT_FAILURE, 0, 0, 1),
                new FailureCase(MessagingErrorCode.THIRD_PARTY_AUTH_ERROR, FcmSendOutcome.PERMANENT_FAILURE, 0, 0, 1)
        );
        return Stream.of(PUSH_MOBILE, PUSH_WEB)
                .flatMap(channel -> cases.stream().map(failure -> Arguments.of(channel, failure)));
    }

    private static Stream<Arguments> topLevelFailures() {
        List<FailureCase> cases = List.of(
                new FailureCase(MessagingErrorCode.UNREGISTERED, FcmSendOutcome.PERMANENT_FAILURE, 0, 0, 1),
                new FailureCase(MessagingErrorCode.INTERNAL, FcmSendOutcome.RETRYABLE_FAILURE, 0, 1, 0),
                new FailureCase(MessagingErrorCode.QUOTA_EXCEEDED, FcmSendOutcome.RETRYABLE_FAILURE, 0, 1, 0),
                new FailureCase(MessagingErrorCode.UNAVAILABLE, FcmSendOutcome.RETRYABLE_FAILURE, 0, 1, 0),
                new FailureCase(MessagingErrorCode.INVALID_ARGUMENT, FcmSendOutcome.PERMANENT_FAILURE, 0, 0, 1),
                new FailureCase(MessagingErrorCode.SENDER_ID_MISMATCH, FcmSendOutcome.PERMANENT_FAILURE, 0, 0, 1),
                new FailureCase(MessagingErrorCode.THIRD_PARTY_AUTH_ERROR, FcmSendOutcome.PERMANENT_FAILURE, 0, 0, 1)
        );
        return Stream.of(PUSH_MOBILE, PUSH_WEB)
                .flatMap(channel -> cases.stream().map(failure -> Arguments.of(channel, failure)));
    }

    private void assertSuccessfulSingleTarget(FcmSendResult result) {
        assertThat(result.outcome()).isEqualTo(FcmSendOutcome.SENT);
        assertThat(result.targetCount()).isOne();
        assertThat(result.successCount()).isOne();
        assertThat(result.invalidTargetCount()).isZero();
        assertThat(result.retryableFailureCount()).isZero();
        assertThat(result.permanentFailureCount()).isZero();
        assertThat(result.errorMessage()).isNull();
    }

    private void assertFailure(FcmSendResult result, FailureCase failure) {
        assertThat(result.outcome()).isEqualTo(failure.outcome());
        assertThat(result.targetCount()).isOne();
        assertThat(result.successCount()).isZero();
        assertThat(result.invalidTargetCount()).isEqualTo(failure.invalidCount());
        assertThat(result.retryableFailureCount()).isEqualTo(failure.retryableCount());
        assertThat(result.permanentFailureCount()).isEqualTo(failure.permanentCount());
        assertThat(result.errorMessage()).contains(failure.errorCode().name());
    }

    private FcmSendResult send(NotificationChannel channel, Notification notification) {
        return channel == PUSH_MOBILE
                ? client.sendNotificationToInstallationMobile(notification, INSTALLATION_ID)
                : client.sendNotificationToInstallationWeb(notification, INSTALLATION_ID);
    }

    private void stubWebLinkIfNeeded(NotificationChannel channel, Notification notification) {
        if (channel == PUSH_WEB) {
            when(notificationResourceLinkResolver.resolve(notification)).thenReturn("https://example.com/events/123");
        }
    }

    private void givenResponses(SendResponse... responses) throws Exception {
        when(batchResponse.getResponses()).thenReturn(List.of(responses));
        when(firebaseMessaging.sendEach(anyList())).thenReturn(batchResponse);
    }

    private SendResponse successfulResponse() {
        SendResponse response = mock(SendResponse.class);
        when(response.isSuccessful()).thenReturn(true);
        return response;
    }

    private SendResponse failedResponse(MessagingErrorCode errorCode) {
        SendResponse response = mock(SendResponse.class);
        FirebaseMessagingException exception = mock(FirebaseMessagingException.class);
        when(response.getException()).thenReturn(exception);
        when(exception.getMessagingErrorCode()).thenReturn(errorCode);
        return response;
    }

    @SuppressWarnings("unchecked")
    private Message sentMessage() throws Exception {
        ArgumentCaptor<List<Message>> messages = ArgumentCaptor.forClass(List.class);
        verify(firebaseMessaging).sendEach(messages.capture());
        assertThat(messages.getValue()).hasSize(1);
        return messages.getValue().getFirst();
    }

    private void assertNotification(Message message, Notification notification) {
        Object fcmNotification = ReflectionTestUtils.getField(message, "notification");
        assertThat(ReflectionTestUtils.getField(fcmNotification, "title")).isEqualTo(notification.getTitle());
        assertThat(ReflectionTestUtils.getField(fcmNotification, "body")).isEqualTo(notification.getBody());
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> messageData(Message message) {
        return (Map<String, String>) ReflectionTestUtils.getField(message, "data");
    }

    private Notification notification(NotificationResourceType resourceType) {
        return Notification.builder()
                .id(UUID.randomUUID())
                .recipientId(UUID.randomUUID())
                .title("Title")
                .body("Body")
                .resourceType(resourceType)
                .resourceId(UUID.randomUUID())
                .createdAt(Instant.parse("2026-01-02T03:04:05Z"))
                .build();
    }

    private record FailureCase(
            MessagingErrorCode errorCode,
            FcmSendOutcome outcome,
            int invalidCount,
            int retryableCount,
            int permanentCount
    ) {
    }
}
