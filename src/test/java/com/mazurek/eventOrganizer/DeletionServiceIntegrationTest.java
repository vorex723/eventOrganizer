package com.mazurek.eventOrganizer;

import com.mazurek.eventOrganizer.auth.ActivationTokenRepository;
import com.mazurek.eventOrganizer.auth.AuthenticationService;
import com.mazurek.eventOrganizer.jwt.DeviceType;
import com.mazurek.eventOrganizer.jwt.RefreshTokenService;
import com.mazurek.eventOrganizer.testData.AuthHelper;
import com.mazurek.eventOrganizer.testData.TestDataInitializer;
import com.mazurek.eventOrganizer.testData.builders.dto.RegisterRequestTestBuilder;
import com.mazurek.eventOrganizer.threadReply.ThreadReplyRepository;
import com.mazurek.eventOrganizer.user.UserRepository;
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
    private com.mazurek.eventOrganizer.conversation.MessageRepository messageRepository;
    @Autowired
    private com.mazurek.eventOrganizer.notification.NotificationRepository notificationRepository;

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

        assertThat(userRepository.count()).isGreaterThan(0L);
        assertThat(eventRepository.count()).isGreaterThan(0L);
        assertThat(threadRepository.count()).isGreaterThan(0L);
        assertThat(threadReplyRepository.count()).isGreaterThan(0L);
        assertThat(fileRepository.count()).isGreaterThan(0L);
        assertThat(tagRepository.count()).isGreaterThan(0L);
        assertThat(activationTokenRepository.count()).isGreaterThan(0L);
        assertThat(refreshTokenRepository.count()).isGreaterThan(0L);

        deletionService.deleteAllSafe();

        assertThat(activationTokenRepository.count()).isEqualTo(0L);
        assertThat(refreshTokenRepository.count()).isEqualTo(0L);
        assertThat(notificationRepository.count()).isEqualTo(0L);
        assertThat(messageRepository.count()).isEqualTo(0L);
        assertThat(conversationRepository.count()).isEqualTo(0L);
        assertThat(tagRepository.count()).isEqualTo(0L);
        assertThat(threadReplyRepository.count()).isEqualTo(0L);
        assertThat(threadRepository.count()).isEqualTo(0L);
        assertThat(fileRepository.count()).isEqualTo(0L);
        assertThat(eventRepository.count()).isEqualTo(0L);
        assertThat(userRepository.count()).isEqualTo(0L);
        assertThat(cityRepository.count()).isEqualTo(0L);
        assertThat(roleRepository.count()).isEqualTo(0L);
    }
}
