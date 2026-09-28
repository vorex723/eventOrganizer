package com.mazurek.eventOrganizer.notification.firebaseCloudMessaging;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.SendResponse;
import com.mazurek.eventOrganizer.notification.domain.DevicePlatform;
import com.mazurek.eventOrganizer.notification.domain.Notification;
import com.mazurek.eventOrganizer.notification.domain.NotificationResourceType;
import com.mazurek.eventOrganizer.notification.repository.NotificationDeviceRepository;
import com.mazurek.eventOrganizer.utils.NotificationResourceLinkResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FcmApiClientProdImplUnitTest {

    @Mock
    private FirebaseMessaging firebaseMessaging;
    @Mock
    private NotificationDeviceRepository notificationDeviceRepository;
    @Mock
    private NotificationResourceLinkResolver notificationResourceLinkResolver;
    @Mock
    private BatchResponse batchResponse;

    private FcmApiClientProdImpl client;

    @BeforeEach
    void setUp() {
        client = new FcmApiClientProdImpl(
                firebaseMessaging,
                notificationDeviceRepository,
                notificationResourceLinkResolver
        );
    }

    @Test
    void noMobileTargetsSkipsFirebaseCall() throws Exception {
        Notification notification = directNotification(NotificationResourceType.EVENT);
        when(notificationDeviceRepository.findAllFirebaseInstallationIdsByUserIdAndPlatformIn(
                notification.getRecipientId(),
                EnumSet.of(DevicePlatform.ANDROID, DevicePlatform.IOS)
        )).thenReturn(List.of());

        FcmSendResult result = client.sendNotificationToSingleUserMobile(notification);

        assertThat(result.outcome()).isEqualTo(FcmSendOutcome.NO_TARGETS);
        assertThat(result.targetCount()).isZero();
        verify(firebaseMessaging, never()).sendEach(anyList());
    }

    @Test
    void mobilePayloadContainsResourceDataAndOmitsMissingParent() throws Exception {
        Notification notification = directNotification(NotificationResourceType.EVENT);
        when(notificationDeviceRepository.findAllFirebaseInstallationIdsByUserIdAndPlatformIn(
                notification.getRecipientId(),
                EnumSet.of(DevicePlatform.ANDROID, DevicePlatform.IOS)
        )).thenReturn(List.of("mobile-fid"));
        SendResponse response = successfulResponse();
        when(batchResponse.getResponses()).thenReturn(List.of(response));
        when(firebaseMessaging.sendEach(anyList())).thenReturn(batchResponse);

        FcmSendResult result = client.sendNotificationToSingleUserMobile(notification);

        assertThat(result.outcome()).isEqualTo(FcmSendOutcome.SENT);
        ArgumentCaptor<List<Message>> messages = messageCaptor();
        verify(firebaseMessaging).sendEach(messages.capture());
        Message message = messages.getValue().getFirst();
        assertThat(ReflectionTestUtils.getField(message, "fid")).isEqualTo("mobile-fid");
        Object fcmNotification = ReflectionTestUtils.getField(message, "notification");
        assertThat(ReflectionTestUtils.getField(fcmNotification, "title")).isEqualTo(notification.getTitle());
        assertThat(ReflectionTestUtils.getField(fcmNotification, "body")).isEqualTo(notification.getBody());
        assertThat(messageData(message)).containsExactlyInAnyOrderEntriesOf(Map.of(
                "notificationId", notification.getId().toString(),
                "resourceType", NotificationResourceType.EVENT.toString(),
                "resourceId", notification.getResourceId().toString()
        ));
    }

    @Test
    void nestedMobilePayloadContainsBothParentFields() throws Exception {
        UUID eventId = UUID.randomUUID();
        Notification notification = directNotification(NotificationResourceType.THREAD);
        notification.setParentResourceType(NotificationResourceType.EVENT);
        notification.setParentResourceId(eventId);
        when(notificationDeviceRepository.findAllFirebaseInstallationIdsByUserIdAndPlatformIn(
                notification.getRecipientId(),
                EnumSet.of(DevicePlatform.ANDROID, DevicePlatform.IOS)
        )).thenReturn(List.of("mobile-fid"));
        SendResponse response = successfulResponse();
        when(batchResponse.getResponses()).thenReturn(List.of(response));
        when(firebaseMessaging.sendEach(anyList())).thenReturn(batchResponse);

        client.sendNotificationToSingleUserMobile(notification);

        ArgumentCaptor<List<Message>> messages = messageCaptor();
        verify(firebaseMessaging).sendEach(messages.capture());
        assertThat(messageData(messages.getValue().getFirst()))
                .containsEntry("parentResourceType", NotificationResourceType.EVENT.toString())
                .containsEntry("parentResourceId", eventId.toString());
    }

    @Test
    void webPayloadContainsNotificationIdAndAbsoluteLink() throws Exception {
        Notification notification = directNotification(NotificationResourceType.EVENT);
        String link = "https://localhost:5173/events/" + notification.getResourceId();
        when(notificationDeviceRepository.findAllFirebaseInstallationIdsByUserIdAndPlatformIn(
                notification.getRecipientId(),
                EnumSet.of(DevicePlatform.WEB)
        )).thenReturn(List.of("web-fid"));
        when(notificationResourceLinkResolver.resolve(notification)).thenReturn(link);
        SendResponse response = successfulResponse();
        when(batchResponse.getResponses()).thenReturn(List.of(response));
        when(firebaseMessaging.sendEach(anyList())).thenReturn(batchResponse);

        client.sendNotificationToSingleUserWeb(notification);

        ArgumentCaptor<List<Message>> messages = messageCaptor();
        verify(firebaseMessaging).sendEach(messages.capture());
        Message message = messages.getValue().getFirst();
        Object fcmNotification = ReflectionTestUtils.getField(message, "notification");
        assertThat(ReflectionTestUtils.getField(fcmNotification, "title")).isEqualTo(notification.getTitle());
        assertThat(ReflectionTestUtils.getField(fcmNotification, "body")).isEqualTo(notification.getBody());
        assertThat(messageData(message)).containsExactly(
                Map.entry("notificationId", notification.getId().toString())
        );
        Object webpushConfig = ReflectionTestUtils.getField(message, "webpushConfig");
        Object fcmOptions = ReflectionTestUtils.getField(webpushConfig, "fcmOptions");
        assertThat(ReflectionTestUtils.getField(fcmOptions, "link")).isEqualTo(link);
    }

    @Test
    void partialSuccessIsSentAndDeletesOnlyUnregisteredInstallation() throws Exception {
        Notification notification = directNotification(NotificationResourceType.EVENT);
        when(notificationDeviceRepository.findAllFirebaseInstallationIdsByUserIdAndPlatformIn(
                notification.getRecipientId(),
                EnumSet.of(DevicePlatform.ANDROID, DevicePlatform.IOS)
        )).thenReturn(List.of("valid-fid", "invalid-fid"));
        SendResponse successfulResponse = successfulResponse();
        SendResponse failedResponse = failedResponse(MessagingErrorCode.UNREGISTERED);
        when(batchResponse.getResponses()).thenReturn(List.of(successfulResponse, failedResponse));
        when(firebaseMessaging.sendEach(anyList())).thenReturn(batchResponse);

        FcmSendResult result = client.sendNotificationToSingleUserMobile(notification);

        assertThat(result.outcome()).isEqualTo(FcmSendOutcome.SENT);
        assertThat(result.successCount()).isOne();
        assertThat(result.invalidTargetCount()).isOne();
        verify(notificationDeviceRepository)
                .deleteAllByFirebaseInstallationIdIn(List.of("invalid-fid"));
    }

    @Test
    void exactlyFiveHundredTargetsAreSentInOneRequest() throws Exception {
        Notification notification = directNotification(NotificationResourceType.EVENT);
        List<String> fids = installationIds(500);
        when(notificationDeviceRepository.findAllFirebaseInstallationIdsByUserIdAndPlatformIn(
                notification.getRecipientId(),
                EnumSet.of(DevicePlatform.ANDROID, DevicePlatform.IOS)
        )).thenReturn(fids);
        List<SendResponse> responses = successfulResponses(500);
        when(batchResponse.getResponses()).thenReturn(responses);
        when(firebaseMessaging.sendEach(anyList())).thenReturn(batchResponse);

        FcmSendResult result = client.sendNotificationToSingleUserMobile(notification);

        ArgumentCaptor<List<Message>> messages = messageCaptor();
        verify(firebaseMessaging).sendEach(messages.capture());
        assertThat(messages.getValue()).hasSize(500);
        assertThat(result.targetCount()).isEqualTo(500);
        assertThat(result.successCount()).isEqualTo(500);
    }

    @Test
    void moreThanOneThousandTargetsAreSplitIntoRequestsOfAtMostFiveHundred() throws Exception {
        Notification notification = directNotification(NotificationResourceType.EVENT);
        List<String> fids = installationIds(1001);
        when(notificationDeviceRepository.findAllFirebaseInstallationIdsByUserIdAndPlatformIn(
                notification.getRecipientId(),
                EnumSet.of(DevicePlatform.ANDROID, DevicePlatform.IOS)
        )).thenReturn(fids);
        BatchResponse firstBatch = successfulBatch(500);
        BatchResponse secondBatch = successfulBatch(500);
        BatchResponse thirdBatch = successfulBatch(1);
        when(firebaseMessaging.sendEach(anyList())).thenReturn(firstBatch, secondBatch, thirdBatch);

        FcmSendResult result = client.sendNotificationToSingleUserMobile(notification);

        ArgumentCaptor<List<Message>> messages = messageCaptor();
        verify(firebaseMessaging, times(3)).sendEach(messages.capture());
        assertThat(messages.getAllValues())
                .extracting(List::size)
                .containsExactly(500, 500, 1);
        assertThat(result.outcome()).isEqualTo(FcmSendOutcome.SENT);
        assertThat(result.successCount()).isEqualTo(1001);
    }

    @Test
    void resultsFromSeparateChunksAreAggregated() throws Exception {
        Notification notification = directNotification(NotificationResourceType.EVENT);
        List<String> fids = installationIds(501);
        when(notificationDeviceRepository.findAllFirebaseInstallationIdsByUserIdAndPlatformIn(
                notification.getRecipientId(),
                EnumSet.of(DevicePlatform.ANDROID, DevicePlatform.IOS)
        )).thenReturn(fids);
        BatchResponse successfulBatch = successfulBatch(500);
        BatchResponse retryableBatch = org.mockito.Mockito.mock(BatchResponse.class);
        List<SendResponse> retryableResponses = List.of(
                failedResponse(MessagingErrorCode.UNAVAILABLE)
        );
        when(retryableBatch.getResponses()).thenReturn(retryableResponses);
        when(firebaseMessaging.sendEach(anyList())).thenReturn(successfulBatch, retryableBatch);

        FcmSendResult result = client.sendNotificationToSingleUserMobile(notification);

        assertThat(result.outcome()).isEqualTo(FcmSendOutcome.SENT);
        assertThat(result.targetCount()).isEqualTo(501);
        assertThat(result.successCount()).isEqualTo(500);
        assertThat(result.retryableFailureCount()).isOne();
    }

    @Test
    void unregisteredTargetInLaterChunkIsMappedToItsInstallationId() throws Exception {
        Notification notification = directNotification(NotificationResourceType.EVENT);
        List<String> fids = installationIds(501);
        when(notificationDeviceRepository.findAllFirebaseInstallationIdsByUserIdAndPlatformIn(
                notification.getRecipientId(),
                EnumSet.of(DevicePlatform.ANDROID, DevicePlatform.IOS)
        )).thenReturn(fids);
        BatchResponse successfulBatch = successfulBatch(500);
        BatchResponse invalidBatch = org.mockito.Mockito.mock(BatchResponse.class);
        List<SendResponse> invalidResponses = List.of(
                failedResponse(MessagingErrorCode.UNREGISTERED)
        );
        when(invalidBatch.getResponses()).thenReturn(invalidResponses);
        when(firebaseMessaging.sendEach(anyList())).thenReturn(successfulBatch, invalidBatch);

        FcmSendResult result = client.sendNotificationToSingleUserMobile(notification);

        assertThat(result.outcome()).isEqualTo(FcmSendOutcome.SENT);
        assertThat(result.invalidTargetCount()).isOne();
        verify(notificationDeviceRepository)
                .deleteAllByFirebaseInstallationIdIn(List.of(fids.get(500)));
    }

    @Test
    void cleanupFailureDoesNotReplaceCompletedFcmResult() throws Exception {
        Notification notification = directNotification(NotificationResourceType.EVENT);
        when(notificationDeviceRepository.findAllFirebaseInstallationIdsByUserIdAndPlatformIn(
                notification.getRecipientId(),
                EnumSet.of(DevicePlatform.ANDROID, DevicePlatform.IOS)
        )).thenReturn(List.of("valid-fid", "invalid-fid"));
        List<SendResponse> responses = List.of(
                successfulResponse(),
                failedResponse(MessagingErrorCode.UNREGISTERED)
        );
        when(batchResponse.getResponses()).thenReturn(responses);
        when(firebaseMessaging.sendEach(anyList())).thenReturn(batchResponse);
        doThrow(new IllegalStateException("Database unavailable."))
                .when(notificationDeviceRepository)
                .deleteAllByFirebaseInstallationIdIn(List.of("invalid-fid"));

        FcmSendResult result = client.sendNotificationToSingleUserMobile(notification);

        assertThat(result.outcome()).isEqualTo(FcmSendOutcome.SENT);
        assertThat(result.successCount()).isOne();
        assertThat(result.invalidTargetCount()).isOne();
    }

    @Test
    void cleanupFailureDoesNotReplaceNoTargetsResultForInvalidInstallations() throws Exception {
        Notification notification = directNotification(NotificationResourceType.EVENT);
        when(notificationDeviceRepository.findAllFirebaseInstallationIdsByUserIdAndPlatformIn(
                notification.getRecipientId(),
                EnumSet.of(DevicePlatform.ANDROID, DevicePlatform.IOS)
        )).thenReturn(List.of("invalid-fid"));
        List<SendResponse> responses = List.of(failedResponse(MessagingErrorCode.UNREGISTERED));
        when(batchResponse.getResponses()).thenReturn(responses);
        when(firebaseMessaging.sendEach(anyList())).thenReturn(batchResponse);
        doThrow(new IllegalStateException("Database unavailable."))
                .when(notificationDeviceRepository)
                .deleteAllByFirebaseInstallationIdIn(List.of("invalid-fid"));

        FcmSendResult result = client.sendNotificationToSingleUserMobile(notification);

        assertThat(result.outcome()).isEqualTo(FcmSendOutcome.NO_TARGETS);
        assertThat(result.invalidTargetCount()).isOne();
    }

    @Test
    void transientTotalFailureIsRetryable() throws Exception {
        FcmSendResult result = sendSingleFailure(MessagingErrorCode.UNAVAILABLE);

        assertThat(result.outcome()).isEqualTo(FcmSendOutcome.RETRYABLE_FAILURE);
        assertThat(result.retryableFailureCount()).isOne();
    }

    @Test
    void permanentTotalFailureIsPermanent() throws Exception {
        FcmSendResult result = sendSingleFailure(MessagingErrorCode.SENDER_ID_MISMATCH);

        assertThat(result.outcome()).isEqualTo(FcmSendOutcome.PERMANENT_FAILURE);
        assertThat(result.permanentFailureCount()).isOne();
    }

    @Test
    void allUnregisteredTargetsBecomeNoTargetsAndAreDeleted() throws Exception {
        FcmSendResult result = sendSingleFailure(MessagingErrorCode.UNREGISTERED);

        assertThat(result.outcome()).isEqualTo(FcmSendOutcome.NO_TARGETS);
        assertThat(result.invalidTargetCount()).isOne();
        verify(notificationDeviceRepository)
                .deleteAllByFirebaseInstallationIdIn(List.of("fid"));
    }

    @Test
    void topLevelTransientFirebaseFailureIsRetryableForEveryTarget() throws Exception {
        Notification notification = directNotification(NotificationResourceType.EVENT);
        when(notificationDeviceRepository.findAllFirebaseInstallationIdsByUserIdAndPlatformIn(
                notification.getRecipientId(),
                EnumSet.of(DevicePlatform.ANDROID, DevicePlatform.IOS)
        )).thenReturn(List.of("first-fid", "second-fid"));
        FirebaseMessagingException exception = org.mockito.Mockito.mock(FirebaseMessagingException.class);
        when(exception.getMessagingErrorCode()).thenReturn(MessagingErrorCode.INTERNAL);
        when(exception.getMessage()).thenReturn("Temporary provider failure.");
        when(firebaseMessaging.sendEach(anyList())).thenThrow(exception);

        FcmSendResult result = client.sendNotificationToSingleUserMobile(notification);

        assertThat(result.outcome()).isEqualTo(FcmSendOutcome.RETRYABLE_FAILURE);
        assertThat(result.targetCount()).isEqualTo(2);
        assertThat(result.retryableFailureCount()).isEqualTo(2);
    }

    private FcmSendResult sendSingleFailure(MessagingErrorCode errorCode) throws Exception {
        Notification notification = directNotification(NotificationResourceType.EVENT);
        when(notificationDeviceRepository.findAllFirebaseInstallationIdsByUserIdAndPlatformIn(
                notification.getRecipientId(),
                EnumSet.of(DevicePlatform.ANDROID, DevicePlatform.IOS)
        )).thenReturn(List.of("fid"));
        SendResponse response = failedResponse(errorCode);
        when(batchResponse.getResponses()).thenReturn(List.of(response));
        when(firebaseMessaging.sendEach(anyList())).thenReturn(batchResponse);

        return client.sendNotificationToSingleUserMobile(notification);
    }

    private SendResponse successfulResponse() {
        SendResponse response = org.mockito.Mockito.mock(SendResponse.class);
        when(response.isSuccessful()).thenReturn(true);
        return response;
    }

    private SendResponse failedResponse(MessagingErrorCode errorCode) {
        FirebaseMessagingException exception = org.mockito.Mockito.mock(FirebaseMessagingException.class);
        when(exception.getMessagingErrorCode()).thenReturn(errorCode);
        SendResponse response = org.mockito.Mockito.mock(SendResponse.class);
        when(response.isSuccessful()).thenReturn(false);
        when(response.getException()).thenReturn(exception);
        return response;
    }

    private BatchResponse successfulBatch(int targetCount) {
        BatchResponse response = org.mockito.Mockito.mock(BatchResponse.class);
        List<SendResponse> responses = successfulResponses(targetCount);
        when(response.getResponses()).thenReturn(responses);
        return response;
    }

    private List<SendResponse> successfulResponses(int targetCount) {
        return Collections.nCopies(targetCount, successfulResponse());
    }

    private List<String> installationIds(int targetCount) {
        return IntStream.range(0, targetCount)
                .mapToObj(index -> "fid-" + index)
                .toList();
    }

    @SuppressWarnings("unchecked")
    private ArgumentCaptor<List<Message>> messageCaptor() {
        return ArgumentCaptor.forClass(List.class);
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> messageData(Message message) {
        return (Map<String, String>) ReflectionTestUtils.getField(message, "data");
    }

    private Notification directNotification(NotificationResourceType resourceType) {
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
}
