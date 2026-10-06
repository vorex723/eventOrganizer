package com.mazurek.eventOrganizer;

import com.mazurek.eventOrganizer.testData.builders.AuthEmailDeliveryTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.NotificationDeliveryTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.NotificationPreferenceTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.NotificationTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.PasswordResetTokenTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.EmailChangeTokenTestBuilder;

import com.mazurek.eventOrganizer.auth.ActivationTokenRepository;
import com.mazurek.eventOrganizer.auth.EmailChangeToken;
import com.mazurek.eventOrganizer.auth.EmailChangeTokenRepository;
import com.mazurek.eventOrganizer.auth.AuthenticationService;
import com.mazurek.eventOrganizer.auth.PasswordResetToken;
import com.mazurek.eventOrganizer.auth.PasswordResetTokenRepository;
import com.mazurek.eventOrganizer.auth.email.AuthEmailDelivery;
import com.mazurek.eventOrganizer.auth.email.AuthEmailDeliveryRepository;
import com.mazurek.eventOrganizer.auth.email.AuthEmailDeliveryStatus;
import com.mazurek.eventOrganizer.auth.email.AuthEmailType;
import com.mazurek.eventOrganizer.city.CityRepository;
import com.mazurek.eventOrganizer.conversation.ConversationCreationService;
import com.mazurek.eventOrganizer.conversation.ConversationRepository;
import com.mazurek.eventOrganizer.conversation.direct.DirectConversationPairRepository;
import com.mazurek.eventOrganizer.conversation.message.MessageRepository;
import com.mazurek.eventOrganizer.conversation.participant.ConversationParticipantRepository;
import com.mazurek.eventOrganizer.event.EventRepository;
import com.mazurek.eventOrganizer.file.FileRepository;
import com.mazurek.eventOrganizer.jwt.DeviceType;
import com.mazurek.eventOrganizer.jwt.RefreshTokenService;
import com.mazurek.eventOrganizer.jwt.RefreshTokenRepository;
import com.mazurek.eventOrganizer.notification.domain.Notification;
import com.mazurek.eventOrganizer.notification.domain.NotificationChannel;
import com.mazurek.eventOrganizer.notification.domain.NotificationDelivery;
import com.mazurek.eventOrganizer.notification.domain.NotificationDeliveryStatus;
import com.mazurek.eventOrganizer.notification.domain.NotificationPreference;
import com.mazurek.eventOrganizer.notification.domain.NotificationResourceType;
import com.mazurek.eventOrganizer.notification.repository.NotificationDeliveryRepository;
import com.mazurek.eventOrganizer.notification.repository.NotificationPreferenceRepository;
import com.mazurek.eventOrganizer.notification.repository.NotificationRepository;
import com.mazurek.eventOrganizer.tag.TagRepository;
import com.mazurek.eventOrganizer.testData.AuthHelper;
import com.mazurek.eventOrganizer.testData.TestDataInitializer;
import com.mazurek.eventOrganizer.testData.builders.dto.RegisterRequestTestBuilder;
import com.mazurek.eventOrganizer.thread.ThreadRepository;
import com.mazurek.eventOrganizer.threadReply.ThreadReplyRepository;
import com.mazurek.eventOrganizer.user.RoleRepository;
import com.mazurek.eventOrganizer.user.User;
import com.mazurek.eventOrganizer.user.UserRepository;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static com.mazurek.eventOrganizer.testData.TestFailureHelper.requirePresent;
import static com.mazurek.eventOrganizer.testData.TestConstants.*;
import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
@DisplayName("DeletionService integration tests:")
class DeletionServiceIntegrationTest {

    @Autowired
    private DeletionService deletionService;
    @Autowired
    private AuthHelper authHelper;
    @Autowired
    private TestDataInitializer testDataInitializer;
    @Autowired
    private AuthenticationService authenticationService;
    @Autowired
    private RefreshTokenService refreshTokenService;
    @Autowired
    private ConversationCreationService conversationCreationService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ActivationTokenRepository activationTokenRepository;
    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;
    @Autowired
    private EmailChangeTokenRepository emailChangeTokenRepository;
    @Autowired
    private AuthEmailDeliveryRepository authEmailDeliveryRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private CityRepository cityRepository;
    @Autowired
    private TagRepository tagRepository;
    @Autowired
    private FileRepository fileRepository;
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private ThreadRepository threadRepository;
    @Autowired
    private ThreadReplyRepository threadReplyRepository;
    @Autowired
    private RefreshTokenRepository refreshTokenRepository;
    @Autowired
    private ConversationRepository conversationRepository;
    @Autowired
    private DirectConversationPairRepository directConversationPairRepository;
    @Autowired
    private ConversationParticipantRepository conversationParticipantRepository;
    @Autowired
    private MessageRepository messageRepository;
    @Autowired
    private NotificationRepository notificationRepository;
    @Autowired
    private NotificationDeliveryRepository notificationDeliveryRepository;
    @Autowired
    private NotificationPreferenceRepository notificationPreferenceRepository;

    @Autowired
    private com.mazurek.eventOrganizer.notification.repository.NotificationDeviceRepository notificationDeviceRepository;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        deletionService.deleteAllSafe();
        authHelper.setupRolesAndUsers();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        deletionService.deleteAllSafe();
    }

    @Test
    @DisplayName("When deleting all data should remove all persisted entities and relations safely")
    void whenDeletingAllDataShouldRemoveAllPersistedEntitiesAndRelationsSafely() throws Exception {
        UUID eventId = testDataInitializer.setupFirstEvent();
        UUID threadId = testDataInitializer.setupThreadInEventByFirstUser(eventId);
        testDataInitializer.setupThreadReplyInThreadByFirstUser(eventId, threadId);
        testDataInitializer.setupFileInEvent(eventId);
        testDataInitializer.addSecondUserToAttendees(eventId);

        authenticationService.register(RegisterRequestTestBuilder.thirdUserRegisterRequest().build());
        User firstUser = requirePresent(userRepository.findByIgnoreCaseEmail(UserConstants.FIRST_USER_EMAIL), "Expected baseline/model prerequisite in whenDeletingAllDataShouldRemoveAllPersistedEntitiesAndRelationsSafely");
        User secondUser = requirePresent(userRepository.findByIgnoreCaseEmail(UserConstants.SECOND_USER_EMAIL), "Expected baseline/model prerequisite in whenDeletingAllDataShouldRemoveAllPersistedEntitiesAndRelationsSafely");
        refreshTokenService.issueRefreshToken(
                firstUser,
                DeviceType.WEB
        );
        Instant now = TimeConstants.NOW;
        // Exactly representable in timestamp(6); exercise a real fractional-second round-trip.
        Instant tokenIssuedAt = now.plusNanos(123_456_000);
        Instant expectedExpiration = tokenIssuedAt.plusMillis(60_000);
        PasswordResetToken passwordResetToken = new PasswordResetTokenTestBuilder().id(null).user(firstUser).unissued().build();
        passwordResetToken.issue(UUID.randomUUID(), 60_000, tokenIssuedAt);
        passwordResetTokenRepository.saveAndFlush(passwordResetToken);

        EmailChangeToken emailChangeToken = new EmailChangeTokenTestBuilder().id(null).user(secondUser).unissued().build();
        emailChangeToken.issue(UUID.randomUUID(), "changed@example.com", 60_000, tokenIssuedAt);
        emailChangeTokenRepository.saveAndFlush(emailChangeToken);

        // Repository calls own separate transactions here; assertions use newly loaded entities.
        PasswordResetToken storedResetToken = requirePresent(passwordResetTokenRepository.findById(passwordResetToken.getId()), "Expected baseline/model prerequisite in whenDeletingAllDataShouldRemoveAllPersistedEntitiesAndRelationsSafely");
        EmailChangeToken storedEmailChangeToken = requirePresent(emailChangeTokenRepository.findById(emailChangeToken.getId()), "Expected baseline/model prerequisite in whenDeletingAllDataShouldRemoveAllPersistedEntitiesAndRelationsSafely");
        assertThat(storedResetToken).isNotSameAs(passwordResetToken);
        assertThat(storedEmailChangeToken).isNotSameAs(emailChangeToken);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(storedResetToken.getExpirationDate()).isEqualTo(expectedExpiration);
            softly.assertThat(storedEmailChangeToken.getExpirationDate()).isEqualTo(expectedExpiration);
            softly.assertThat(storedResetToken.isExpired(expectedExpiration.minusNanos(1))).isFalse();
            softly.assertThat(storedResetToken.isExpired(expectedExpiration)).isTrue();
            softly.assertThat(storedResetToken.isExpired(expectedExpiration.plusNanos(1))).isTrue();
            softly.assertThat(storedEmailChangeToken.isExpired(expectedExpiration.minusNanos(1))).isFalse();
            softly.assertThat(storedEmailChangeToken.isExpired(expectedExpiration)).isTrue();
            softly.assertThat(storedEmailChangeToken.isExpired(expectedExpiration.plusNanos(1))).isTrue();
        });

        authEmailDeliveryRepository.saveAndFlush(new AuthEmailDeliveryTestBuilder().id(null)
                .userId(firstUser.getId())
                .recipientEmail(firstUser.getEmail())
                .type(AuthEmailType.ACCOUNT_ACTIVATION)
                .encryptedToken("test-token")
                .status(AuthEmailDeliveryStatus.PENDING)
                .createdAt(now)
                .build());

        conversationCreationService.createDirectConversationWithInitialMessage(
                firstUser, secondUser, "Test message", now);

        notificationPreferenceRepository.saveAndFlush(new NotificationPreferenceTestBuilder().id(null)
                .userId(firstUser.getId())
                .resourceType(NotificationResourceType.EVENT)
                .channel(NotificationChannel.EMAIL)
                .enabled(true)
                .build());

        Notification notification = new NotificationTestBuilder().id(null)
                .recipientId(firstUser.getId())
                .resourceType(NotificationResourceType.EVENT)
                .resourceId(eventId)
                .createdAt(now)
                .build();
        notification.addDelivery(new NotificationDeliveryTestBuilder().id(null)
                .channel(NotificationChannel.EMAIL)
                .targetKey(firstUser.getEmail())
                .targetEmail(firstUser.getEmail())
                .status(NotificationDeliveryStatus.PENDING)
                .createdAt(now)
                .notification(null)
                .build());
        notificationRepository.saveAndFlush(notification);

        notificationDeviceRepository.saveAndFlush(new com.mazurek.eventOrganizer.testData.builders.NotificationDeviceTestBuilder()
                .id(null).userId(firstUser.getId()).build());

        assertRepositoryCounts(false);

        deletionService.deleteAllSafe();

        assertRepositoryCounts(true);
    }

    private void assertRepositoryCounts(boolean empty) {
        SoftAssertions.assertSoftly(softly -> repositoryCounts().forEach((name, count) -> {
            if (empty) {
                softly.assertThat(count).as(name).isZero();
            } else {
                softly.assertThat(count).as(name).isPositive();
            }
        }));
    }

    private Map<String, Long> repositoryCounts() {
        return Map.ofEntries(
                Map.entry("activation tokens", activationTokenRepository.count()),
                Map.entry("password reset tokens", passwordResetTokenRepository.count()),
                Map.entry("email change tokens", emailChangeTokenRepository.count()),
                Map.entry("auth email deliveries", authEmailDeliveryRepository.count()),
                Map.entry("roles", roleRepository.count()),
                Map.entry("cities", cityRepository.count()),
                Map.entry("tags", tagRepository.count()),
                Map.entry("files", fileRepository.count()),
                Map.entry("events", eventRepository.count()),
                Map.entry("users", userRepository.count()),
                Map.entry("conversations", conversationRepository.count()),
                Map.entry("direct conversation pairs", directConversationPairRepository.count()),
                Map.entry("conversation participants", conversationParticipantRepository.count()),
                Map.entry("messages", messageRepository.count()),
                Map.entry("notifications", notificationRepository.count()),
                Map.entry("notification deliveries", notificationDeliveryRepository.count()),
                Map.entry("notification preferences", notificationPreferenceRepository.count()),
                Map.entry("notification devices", notificationDeviceRepository.count()),
                Map.entry("refresh tokens", refreshTokenRepository.count()),
                Map.entry("threads", threadRepository.count()),
                Map.entry("thread replies", threadReplyRepository.count())
        );
    }
}
