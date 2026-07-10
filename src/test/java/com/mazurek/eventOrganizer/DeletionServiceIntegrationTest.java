package com.mazurek.eventOrganizer;

import com.mazurek.eventOrganizer.auth.ActivationTokenRepository;
import com.mazurek.eventOrganizer.auth.AuthenticationService;
import com.mazurek.eventOrganizer.conversation.message.MessageRepository;
import com.mazurek.eventOrganizer.jwt.DeviceType;
import com.mazurek.eventOrganizer.jwt.RefreshTokenService;
import com.mazurek.eventOrganizer.notification.repository.NotificationRepository;
import com.mazurek.eventOrganizer.testData.AuthHelper;
import com.mazurek.eventOrganizer.testData.TestDataInitializer;
import com.mazurek.eventOrganizer.testData.builders.dto.RegisterRequestTestBuilder;
import com.mazurek.eventOrganizer.threadReply.ThreadReplyRepository;
import com.mazurek.eventOrganizer.user.UserRepository;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static com.mazurek.eventOrganizer.testData.TestConstants.*;
import static org.assertj.core.api.Assertions.assertThat;

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
    private UserRepository userRepository;
    @Autowired
    private ActivationTokenRepository activationTokenRepository;
    @Autowired
    private com.mazurek.eventOrganizer.user.RoleRepository roleRepository;
    @Autowired
    private com.mazurek.eventOrganizer.city.CityRepository cityRepository;
    @Autowired
    private com.mazurek.eventOrganizer.tag.TagRepository tagRepository;
    @Autowired
    private com.mazurek.eventOrganizer.event.EventRepository eventRepository;
    @Autowired
    private com.mazurek.eventOrganizer.thread.ThreadRepository threadRepository;
    @Autowired
    private ThreadReplyRepository threadReplyRepository;
    @Autowired
    private com.mazurek.eventOrganizer.file.FileRepository fileRepository;
    @Autowired
    private com.mazurek.eventOrganizer.jwt.RefreshTokenRepository refreshTokenRepository;
    @Autowired
    private com.mazurek.eventOrganizer.conversation.ConversationRepository conversationRepository;
    @Autowired
    private MessageRepository messageRepository;
    @Autowired
    private NotificationRepository notificationRepository;

    @BeforeEach
    void setUp() {
        deletionService.deleteAllSafe();
        authHelper.setupRolesAndUsers();
    }

    @Test
    @DisplayName("When deleting all data should remove all persisted entities and relations safely")
    void whenDeletingAllDataShouldRemoveAllPersistedEntitiesAndRelationsSafely() throws Exception {
        UUID eventId = testDataInitializer.setupFirstEvent();
        UUID threadId = testDataInitializer.setupThreadInEventByFirstUser(eventId);
        testDataInitializer.setupThreadReplyInThreadByFirstUser(eventId, threadId);
        testDataInitializer.setupFileInEvent(eventId);

        authenticationService.register(RegisterRequestTestBuilder.thirdUserRegisterRequest().build());
        refreshTokenService.createRefreshToken(
                userRepository.findByIgnoreCaseEmail(UserConstants.FIRST_USER_EMAIL).orElseThrow(),
                DeviceType.WEB
        );

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(userRepository.count()).isGreaterThan(0L);
            softly.assertThat(eventRepository.count()).isGreaterThan(0L);
            softly.assertThat(threadRepository.count()).isGreaterThan(0L);
            softly.assertThat(threadReplyRepository.count()).isGreaterThan(0L);
            softly.assertThat(fileRepository.count()).isGreaterThan(0L);
            softly.assertThat(tagRepository.count()).isGreaterThan(0L);
            softly.assertThat(activationTokenRepository.count()).isGreaterThan(0L);
            softly.assertThat(refreshTokenRepository.count()).isGreaterThan(0L);
        });

        deletionService.deleteAllSafe();

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(activationTokenRepository.count()).isEqualTo(0L);
            softly.assertThat(refreshTokenRepository.count()).isEqualTo(0L);
            softly.assertThat(notificationRepository.count()).isEqualTo(0L);
            softly.assertThat(messageRepository.count()).isEqualTo(0L);
            softly.assertThat(conversationRepository.count()).isEqualTo(0L);
            softly.assertThat(tagRepository.count()).isEqualTo(0L);
            softly.assertThat(threadReplyRepository.count()).isEqualTo(0L);
            softly.assertThat(threadRepository.count()).isEqualTo(0L);
            softly.assertThat(fileRepository.count()).isEqualTo(0L);
            softly.assertThat(eventRepository.count()).isEqualTo(0L);
            softly.assertThat(userRepository.count()).isEqualTo(0L);
            softly.assertThat(cityRepository.count()).isEqualTo(0L);
            softly.assertThat(roleRepository.count()).isEqualTo(0L);
        });
    }
}
