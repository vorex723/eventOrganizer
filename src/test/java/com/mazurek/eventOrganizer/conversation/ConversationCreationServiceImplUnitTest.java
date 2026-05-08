package com.mazurek.eventOrganizer.conversation;

import com.mazurek.eventOrganizer.conversation.participant.ConversationParticipant;
import com.mazurek.eventOrganizer.conversation.participant.ConversationParticipantRepository;
import com.mazurek.eventOrganizer.conversation.direct.DirectConversationPair;
import com.mazurek.eventOrganizer.conversation.direct.DirectConversationPairRepository;
import com.mazurek.eventOrganizer.testData.builders.UserTestBuilder;
import com.mazurek.eventOrganizer.user.User;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static com.mazurek.eventOrganizer.testData.TestConstants.*;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConversationCreationServiceImpl unit tests:")
class ConversationCreationServiceImplUnitTest {

    @Mock
    private ConversationRepository conversationRepository;
    @Mock
    private ConversationParticipantRepository participantRepository;
    @Mock
    private DirectConversationPairRepository directConversationPairRepository;

    @InjectMocks
    private ConversationCreationServiceImpl conversationCreationService;

    private User firstUser;
    private User secondUser;
    private Instant createdAt;

    @BeforeEach
    void setUp() {
        firstUser = UserTestBuilder.firstUser().build();
        secondUser = UserTestBuilder.secondUser().build();
        createdAt = TimeConstants.NOW;
    }

    @Nested
    @DisplayName("Create direct conversation tests:")
    class CreateDirectConversationTests {

        @Test
        @DisplayName("When creating direct conversation should save conversation participants and direct pair")
        void whenCreatingDirectConversationShouldSaveConversationParticipantsAndDirectPair() {
            setupSuccessfulRepositorySaves();

            UUID conversationId = conversationCreationService.createDirectConversation(firstUser, secondUser, createdAt);

            ArgumentCaptor<Conversation> conversationCaptor = ArgumentCaptor.forClass(Conversation.class);
            ArgumentCaptor<ConversationParticipant> participantCaptor = ArgumentCaptor.forClass(ConversationParticipant.class);
            ArgumentCaptor<DirectConversationPair> directPairCaptor = ArgumentCaptor.forClass(DirectConversationPair.class);

            verify(conversationRepository, times(1)).save(conversationCaptor.capture());
            verify(participantRepository, times(2)).save(participantCaptor.capture());
            verify(directConversationPairRepository, times(1)).saveAndFlush(directPairCaptor.capture());

            Conversation savedConversation = conversationCaptor.getValue();
            List<ConversationParticipant> savedParticipants = participantCaptor.getAllValues();
            DirectConversationPair savedDirectPair = directPairCaptor.getValue();

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(conversationId).isEqualTo(ConversationConstants.FIRST_CONVERSATION_ID);
                softly.assertThat(savedConversation.getId()).isEqualTo(ConversationConstants.FIRST_CONVERSATION_ID);
                softly.assertThat(savedConversation.getType()).isEqualTo(ConversationType.DIRECT);
                softly.assertThat(savedConversation.getCreatedAt()).isEqualTo(createdAt);
                softly.assertThat(savedConversation.getLastActiveAt()).isEqualTo(createdAt);
                softly.assertThat(savedConversation.getParticipants()).containsExactlyInAnyOrderElementsOf(savedParticipants);
            });

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(savedParticipants).extracting(participant -> participant.getUser().getId())
                        .containsExactlyInAnyOrder(UserConstants.FIRST_USER_ID, UserConstants.SECOND_USER_ID);
                softly.assertThat(savedParticipants).extracting(ConversationParticipant::getConversation)
                        .containsOnly(savedConversation);
                softly.assertThat(savedParticipants).extracting(ConversationParticipant::getJoinedAt)
                        .containsOnly(createdAt);
                softly.assertThat(savedParticipants).extracting(ConversationParticipant::getLeftAt)
                        .containsOnlyNulls();
                softly.assertThat(savedParticipants).extracting(ConversationParticipant::getLastReadAt)
                        .containsOnlyNulls();
                softly.assertThat(savedParticipants).extracting(ConversationParticipant::getLastReadMessageId)
                        .containsOnlyNulls();
            });

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(savedDirectPair.getConversation()).isSameAs(savedConversation);
                softly.assertThat(savedDirectPair.getFirstUserId()).isEqualTo(UserConstants.FIRST_USER_ID);
                softly.assertThat(savedDirectPair.getSecondUserId()).isEqualTo(UserConstants.SECOND_USER_ID);
            });
        }

        @Test
        @DisplayName("When creating direct conversation in inverse direction should save canonical direct pair")
        void whenCreatingDirectConversationInInverseDirectionShouldSaveCanonicalDirectPair() {
            setupSuccessfulRepositorySaves();

            conversationCreationService.createDirectConversation(secondUser, firstUser, createdAt);

            ArgumentCaptor<DirectConversationPair> directPairCaptor = ArgumentCaptor.forClass(DirectConversationPair.class);
            verify(directConversationPairRepository, times(1)).saveAndFlush(directPairCaptor.capture());

            DirectConversationPair savedDirectPair = directPairCaptor.getValue();

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(savedDirectPair.getFirstUserId()).isEqualTo(UserConstants.FIRST_USER_ID);
                softly.assertThat(savedDirectPair.getSecondUserId()).isEqualTo(UserConstants.SECOND_USER_ID);
            });
        }
        @Test
        @DisplayName("When direct pair save fails should propagate DataIntegrityViolationException")
        void whenDirectPairSaveFailsShouldPropagateDataIntegrityViolationException() {
            setupSuccessfulConversationAndParticipantSaves();
            when(directConversationPairRepository.saveAndFlush(any(DirectConversationPair.class)))
                    .thenThrow(new DataIntegrityViolationException("duplicate direct conversation pair"));

            assertThatThrownBy(() -> conversationCreationService.createDirectConversation(firstUser, secondUser, createdAt))
                    .isInstanceOf(DataIntegrityViolationException.class);

            verify(conversationRepository, times(1)).save(any(Conversation.class));
            verify(participantRepository, times(2)).save(any(ConversationParticipant.class));
            verify(directConversationPairRepository, times(1)).saveAndFlush(any(DirectConversationPair.class));
        }

        @Test
        @DisplayName("When creating direct conversation should run in a new transaction")
        void whenCreatingDirectConversationShouldRunInNewTransaction() throws NoSuchMethodException {
            Method method = ConversationCreationServiceImpl.class.getMethod(
                    "createDirectConversation",
                    User.class,
                    User.class,
                    Instant.class);

            Transactional transactional = method.getAnnotation(Transactional.class);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(transactional).isNotNull();
                softly.assertThat(transactional.propagation()).isEqualTo(Propagation.REQUIRES_NEW);
            });
        }
    }

    private void setupSuccessfulRepositorySaves() {
        setupSuccessfulConversationAndParticipantSaves();
        when(directConversationPairRepository.saveAndFlush(any(DirectConversationPair.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private void setupSuccessfulConversationAndParticipantSaves() {
        when(conversationRepository.save(any(Conversation.class))).thenAnswer(invocation -> {
            Conversation conversation = invocation.getArgument(0);
            conversation.setId(ConversationConstants.FIRST_CONVERSATION_ID);
            return conversation;
        });
        when(participantRepository.save(any(ConversationParticipant.class))).thenAnswer(invocation -> {
            ConversationParticipant participant = invocation.getArgument(0);
            if (participant.getUser().getId().equals(UserConstants.FIRST_USER_ID)) {
                participant.setId(ConversationParticipantConstants.FIRST_CONVERSATION_PARTICIPANT_ID);
            } else {
                participant.setId(ConversationParticipantConstants.SECOND_CONVERSATION_PARTICIPANT_ID);
            }
            return participant;
        });
    }
}
