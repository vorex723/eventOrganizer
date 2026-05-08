package com.mazurek.eventOrganizer.conversation;

import com.mazurek.eventOrganizer.DeletionService;
import com.mazurek.eventOrganizer.conversation.participant.ConversationParticipant;
import com.mazurek.eventOrganizer.conversation.participant.ConversationParticipantRepository;
import com.mazurek.eventOrganizer.conversation.direct.DirectConversationPair;
import com.mazurek.eventOrganizer.conversation.direct.DirectConversationPairRepository;
import com.mazurek.eventOrganizer.exception.user.UserNotFoundException;
import com.mazurek.eventOrganizer.testData.AuthHelper;
import com.mazurek.eventOrganizer.user.User;
import com.mazurek.eventOrganizer.user.UserRepository;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static com.mazurek.eventOrganizer.testData.TestConstants.TimeConstants;
import static com.mazurek.eventOrganizer.testData.TestConstants.UserConstants;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Profile("test")
@DisplayName("ConversationCreationService integration tests:")
class ConversationCreationServiceImplIntegrationTest {

    @Autowired
    private ConversationCreationService conversationCreationService;
    @Autowired
    private ConversationRepository conversationRepository;
    @Autowired
    private ConversationParticipantRepository conversationParticipantRepository;
    @Autowired
    private DirectConversationPairRepository directConversationPairRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AuthHelper authHelper;
    @Autowired
    private DeletionService deletionService;

    private User firstUser;
    private User secondUser;

    @BeforeEach
    void setUp() {
        deletionService.deleteAllSafe();
        authHelper.setupRolesAndUsers();

        firstUser = userRepository.findByEmail(UserConstants.FIRST_USER_EMAIL)
                .orElseThrow(UserNotFoundException::new);
        secondUser = userRepository.findByEmail(UserConstants.SECOND_USER_EMAIL)
                .orElseThrow(UserNotFoundException::new);
    }

    @AfterEach
    void tearDown() {
        deletionService.deleteAllSafe();
    }

    @Nested
    @DisplayName("Create direct conversation tests:")
    class CreateDirectConversationTests {

        @Test
        @DisplayName("When creating direct conversation should persist conversation participants and direct pair")
        void whenCreatingDirectConversationShouldPersistConversationParticipantsAndDirectPair() {
            UUID conversationId = conversationCreationService.createDirectConversation(firstUser, secondUser, TimeConstants.NOW);

            List<Conversation> conversations = conversationRepository.findAll();
            List<ConversationParticipant> participants = conversationParticipantRepository.findAll();
            List<DirectConversationPair> directConversationPairs = directConversationPairRepository.findAll();

            Conversation savedConversation = conversations.getFirst();
            DirectConversationPair savedDirectConversationPair = directConversationPairs.getFirst();
            ConversationParticipant firstUserParticipant = findParticipant(participants, firstUser);
            ConversationParticipant secondUserParticipant = findParticipant(participants, secondUser);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(conversationId).isEqualTo(savedConversation.getId());
                softly.assertThat(conversations).hasSize(1);
                softly.assertThat(savedConversation.getType()).isEqualTo(ConversationType.DIRECT);
                softly.assertThat(savedConversation.getCreatedAt()).isEqualTo(TimeConstants.NOW);
                softly.assertThat(savedConversation.getLastActiveAt()).isEqualTo(TimeConstants.NOW);
            });

            assertParticipants(savedConversation.getId(), participants, firstUserParticipant, secondUserParticipant, TimeConstants.NOW);
            assertDirectConversationPair(savedDirectConversationPair, savedConversation.getId(), firstUser, secondUser);
        }

        @Test
        @DisplayName("When creating direct conversation in inverse direction should persist canonical direct pair")
        void whenCreatingDirectConversationInInverseDirectionShouldPersistCanonicalDirectPair() {
            UUID conversationId = conversationCreationService.createDirectConversation(secondUser, firstUser, TimeConstants.NOW);

            List<ConversationParticipant> participants = conversationParticipantRepository.findAll();
            DirectConversationPair savedDirectConversationPair = directConversationPairRepository.findAll().getFirst();

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(conversationRepository.findAll()).hasSize(1);
                softly.assertThat(participants).hasSize(2);
                softly.assertThat(savedDirectConversationPair.getConversation().getId()).isEqualTo(conversationId);
                softly.assertThat(savedDirectConversationPair.getFirstUserId()).isEqualTo(canonicalFirstUserId(firstUser, secondUser));
                softly.assertThat(savedDirectConversationPair.getSecondUserId()).isEqualTo(canonicalSecondUserId(firstUser, secondUser));
                softly.assertThat(participants).extracting(participant -> participant.getUser().getId())
                        .containsExactlyInAnyOrder(firstUser.getId(), secondUser.getId());
            });
        }
        @Test
        @DisplayName("When creating duplicate direct conversation should throw and rollback failed creation")
        void whenCreatingDuplicateDirectConversationShouldThrowAndRollbackFailedCreation() {
            conversationCreationService.createDirectConversation(firstUser, secondUser, TimeConstants.NOW);

            assertThatThrownBy(() -> conversationCreationService.createDirectConversation(firstUser, secondUser, TimeConstants.ONE_HOUR_AGO))
                    .isInstanceOf(DataIntegrityViolationException.class);

            assertOnlyOriginalDirectConversationRemains();
        }

        @Test
        @DisplayName("When creating duplicate direct conversation in inverse direction should throw and rollback failed creation")
        void whenCreatingDuplicateDirectConversationInInverseDirectionShouldThrowAndRollbackFailedCreation() {
            conversationCreationService.createDirectConversation(firstUser, secondUser, TimeConstants.NOW);

            assertThatThrownBy(() -> conversationCreationService.createDirectConversation(secondUser, firstUser, TimeConstants.ONE_HOUR_AGO))
                    .isInstanceOf(DataIntegrityViolationException.class);

            assertOnlyOriginalDirectConversationRemains();
        }
    }

    private ConversationParticipant findParticipant(List<ConversationParticipant> participants, User user) {
        return participants.stream()
                .filter(participant -> participant.getUser().getId().equals(user.getId()))
                .findFirst()
                .orElseThrow();
    }

    private UUID canonicalFirstUserId(User userA, User userB) {
        return userA.getId().toString().compareTo(userB.getId().toString()) < 0
                ? userA.getId()
                : userB.getId();
    }

    private UUID canonicalSecondUserId(User userA, User userB) {
        return userA.getId().toString().compareTo(userB.getId().toString()) < 0
                ? userB.getId()
                : userA.getId();
    }

    private void assertParticipants(
            UUID conversationId,
            List<ConversationParticipant> participants,
            ConversationParticipant firstUserParticipant,
            ConversationParticipant secondUserParticipant,
            Instant joinedAt) {
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(participants).hasSize(2);
            softly.assertThat(firstUserParticipant.getConversation().getId()).isEqualTo(conversationId);
            softly.assertThat(firstUserParticipant.getUser().getId()).isEqualTo(firstUser.getId());
            softly.assertThat(firstUserParticipant.getJoinedAt()).isEqualTo(joinedAt);
            softly.assertThat(firstUserParticipant.getLastReadAt()).isNull();
            softly.assertThat(firstUserParticipant.getLastReadMessageId()).isNull();
            softly.assertThat(secondUserParticipant.getConversation().getId()).isEqualTo(conversationId);
            softly.assertThat(secondUserParticipant.getUser().getId()).isEqualTo(secondUser.getId());
            softly.assertThat(secondUserParticipant.getJoinedAt()).isEqualTo(joinedAt);
            softly.assertThat(secondUserParticipant.getLastReadAt()).isNull();
            softly.assertThat(secondUserParticipant.getLastReadMessageId()).isNull();
        });
    }

    private void assertDirectConversationPair(
            DirectConversationPair directConversationPair,
            UUID conversationId,
            User firstUser,
            User secondUser) {
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(directConversationPairRepository.findAll()).hasSize(1);
            softly.assertThat(directConversationPair.getConversation().getId()).isEqualTo(conversationId);
            softly.assertThat(directConversationPair.getFirstUserId()).isEqualTo(canonicalFirstUserId(firstUser, secondUser));
            softly.assertThat(directConversationPair.getSecondUserId()).isEqualTo(canonicalSecondUserId(firstUser, secondUser));
        });
    }

    private void assertOnlyOriginalDirectConversationRemains() {
        List<Conversation> conversations = conversationRepository.findAll();
        List<ConversationParticipant> participants = conversationParticipantRepository.findAll();
        List<DirectConversationPair> directConversationPairs = directConversationPairRepository.findAll();

        Conversation savedConversation = conversations.getFirst();
        DirectConversationPair savedDirectConversationPair = directConversationPairs.getFirst();

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(conversations).hasSize(1);
            softly.assertThat(participants).hasSize(2);
            softly.assertThat(directConversationPairs).hasSize(1);
            softly.assertThat(savedConversation.getCreatedAt()).isEqualTo(TimeConstants.NOW);
            softly.assertThat(savedConversation.getLastActiveAt()).isEqualTo(TimeConstants.NOW);
        });

        assertDirectConversationPair(savedDirectConversationPair, savedConversation.getId(), firstUser, secondUser);
    }
}
