package com.mazurek.eventOrganizer.testData;

import com.mazurek.eventOrganizer.event.EventService;
import com.mazurek.eventOrganizer.file.FileService;
import com.mazurek.eventOrganizer.testData.builders.dto.EventDtoTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.dto.FileOverviewDtoTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.dto.ThreadDtoTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.dto.ThreadReplyDtoTestBuilder;
import com.mazurek.eventOrganizer.thread.ThreadService;
import com.mazurek.eventOrganizer.threadReply.ThreadReplyService;
import com.mazurek.eventOrganizer.threadReply.dto.ThreadReplyCreateDto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static com.mazurek.eventOrganizer.testData.TestConstants.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TestDataInitializer unit tests:")
class TestDataInitializerUnitTest {
    @InjectMocks private TestDataInitializer initializer;
    @Mock private EventService eventService;
    @Mock private ThreadService threadService;
    @Mock private ThreadReplyService threadReplyService;
    @Mock private FileService fileService;
    @Mock private AuthHelper authHelper;

    @BeforeEach
    void setUp() {
        authenticate(UserConstants.THIRD_USER_EMAIL);
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @ParameterizedTest(name = "Operation: {0}")
    @EnumSource(Operation.class)
    void whenSetupSucceedsShouldUseRequestedActorReturnResultAndLeaveEmptyContext(Operation operation) throws Exception {
        configureActor(operation, null);
        configureService(operation, null);

        Object result = invoke(operation);

        assertThat(result).isEqualTo(expectedResult(operation));
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        int expectedActorSetups = operation == Operation.REPLIES_FIRST || operation == Operation.REPLIES_SECOND ? 2 : 1;
        if (isSecondUser(operation)) {
            verify(authHelper, times(expectedActorSetups)).setupSecurityContextForSecondUser();
        } else {
            verify(authHelper, times(expectedActorSetups)).setupSecurityContextForFirstUser();
        }
        verifyNoMoreInteractions(authHelper);
    }

    @ParameterizedTest(name = "Operation: {0}")
    @EnumSource(Operation.class)
    void whenServiceFailsShouldPropagateFailureAndLeaveEmptyContext(Operation operation) throws Exception {
        IllegalStateException failure = new IllegalStateException("Fixture service failure");
        configureActor(operation, null);
        configureService(operation, failure);

        assertThatThrownBy(() -> invoke(operation)).isSameAs(failure);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @ParameterizedTest(name = "Operation: {0}")
    @EnumSource(Operation.class)
    void whenActorSetupFailsShouldClearPartialAuthenticationWithoutCallingServices(Operation operation) throws Exception {
        IllegalStateException failure = new IllegalStateException("Fixture authentication failure");
        configureActor(operation, failure);

        assertThatThrownBy(() -> invoke(operation)).isSameAs(failure);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(eventService, threadService, threadReplyService, fileService);
    }

    @Test
    void whenFileServiceThrowsCheckedExceptionShouldPropagateItAndClearAuthentication() throws Exception {
        IOException failure = new IOException("Fixture upload failure");
        configureActor(Operation.FILE, null);
        configureService(Operation.FILE, failure);

        assertThatThrownBy(() -> initializer.setupFileInEvent(EventConstants.FIRST_EVENT_ID)).isSameAs(failure);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @ParameterizedTest(name = "Second user: {0}")
    @ValueSource(booleans = {false, true})
    void whenCreatingNoRepliesShouldReturnEmptyListAndClearExistingActor(boolean secondUser) {
        List<UUID> result = secondUser
                ? initializer.setupThreadRepliesInThreadBySecondUser(EventConstants.FIRST_EVENT_ID, ThreadConstants.FIRST_THREAD_ID, 0)
                : initializer.setupThreadRepliesInThreadByFirstUser(EventConstants.FIRST_EVENT_ID, ThreadConstants.FIRST_THREAD_ID, 0);

        assertThat(result).isEmpty();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(authHelper, threadReplyService);
    }

    @ParameterizedTest(name = "Second user: {0}")
    @ValueSource(booleans = {false, true})
    void whenLaterReplyFailsShouldRetainNumberedRequestsAndClearAuthentication(boolean secondUser) throws Exception {
        Operation operation = secondUser ? Operation.REPLIES_SECOND : Operation.REPLIES_FIRST;
        configureActor(operation, null);
        AtomicInteger calls = new AtomicInteger();
        IllegalStateException failure = new IllegalStateException("Second reply failed");
        doAnswer(invocation -> {
            assertActor(operation);
            if (calls.incrementAndGet() == 2) throw failure;
            return new ThreadReplyDtoTestBuilder().build();
        }).when(threadReplyService).createReplyInThread(any(), eq(EventConstants.FIRST_EVENT_ID), eq(ThreadConstants.FIRST_THREAD_ID));

        assertThatThrownBy(() -> invoke(operation)).isSameAs(failure);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        ArgumentCaptor<ThreadReplyCreateDto> requests = ArgumentCaptor.forClass(ThreadReplyCreateDto.class);
        verify(threadReplyService, times(2)).createReplyInThread(requests.capture(), eq(EventConstants.FIRST_EVENT_ID), eq(ThreadConstants.FIRST_THREAD_ID));
        assertThat(requests.getAllValues()).extracting(ThreadReplyCreateDto::getReplyContent)
                .containsExactly(ThreadReplyConstants.FIRST_REPLY_CONTENT + " 0", ThreadReplyConstants.FIRST_REPLY_CONTENT + " 1");
    }

    private void configureActor(Operation operation, RuntimeException failure) {
        var answer = doAnswer(invocation -> {
            authenticate(actorEmail(operation));
            if (failure != null) throw failure;
            return null;
        });
        if (isSecondUser(operation)) {
            answer.when(authHelper).setupSecurityContextForSecondUser();
        } else {
            answer.when(authHelper).setupSecurityContextForFirstUser();
        }
    }

    private void configureService(Operation operation, Exception failure) throws IOException {
        AtomicInteger calls = new AtomicInteger();
        var answer = doAnswer(invocation -> {
            assertActor(operation);
            int callIndex = calls.getAndIncrement();
            if (operation == Operation.REPLY_FIRST || operation == Operation.REPLY_SECOND
                    || operation == Operation.CUSTOM_REPLY_FIRST || operation == Operation.CUSTOM_REPLY_SECOND
                    || operation == Operation.REPLIES_FIRST || operation == Operation.REPLIES_SECOND) {
                ThreadReplyCreateDto request = invocation.getArgument(0);
                String content = switch (operation) {
                    case CUSTOM_REPLY_FIRST, CUSTOM_REPLY_SECOND -> ThreadReplyConstants.FIRST_REPLY_CONTENT + " custom";
                    case REPLIES_FIRST, REPLIES_SECOND -> ThreadReplyConstants.FIRST_REPLY_CONTENT + " " + callIndex;
                    default -> ThreadReplyConstants.FIRST_REPLY_CONTENT;
                };
                assertThat(request.getReplyContent()).isEqualTo(content);
            }
            if (failure != null) throw failure;
            return switch (operation) {
                case FIRST_EVENT, EVENT_FIRST, EVENT_SECOND -> new EventDtoTestBuilder().build();
                case THREAD_FIRST, THREAD_SECOND -> new ThreadDtoTestBuilder().build();
                case REPLY_FIRST, REPLY_SECOND, CUSTOM_REPLY_FIRST, CUSTOM_REPLY_SECOND -> new ThreadReplyDtoTestBuilder().build();
                case REPLIES_FIRST, REPLIES_SECOND -> new ThreadReplyDtoTestBuilder()
                        .id(callIndex == 0 ? ThreadReplyConstants.FIRST_REPLY_ID : ThreadReplyConstants.SECOND_REPLY_ID).build();
                case FILE -> new FileOverviewDtoTestBuilder().build();
                case ATTENDANCE_FIRST, ATTENDANCE_SECOND -> null;
            };
        });
        switch (operation) {
            case FIRST_EVENT, EVENT_FIRST, EVENT_SECOND -> answer.when(eventService).createEvent(any());
            case THREAD_FIRST, THREAD_SECOND -> answer.when(threadService).createThreadInEvent(any(), eq(EventConstants.FIRST_EVENT_ID));
            case REPLY_FIRST, REPLY_SECOND, CUSTOM_REPLY_FIRST, CUSTOM_REPLY_SECOND, REPLIES_FIRST, REPLIES_SECOND ->
                    answer.when(threadReplyService).createReplyInThread(any(), eq(EventConstants.FIRST_EVENT_ID), eq(ThreadConstants.FIRST_THREAD_ID));
            case FILE -> answer.when(fileService).uploadFileToEvent(any(), eq(EventConstants.FIRST_EVENT_ID));
            case ATTENDANCE_FIRST, ATTENDANCE_SECOND -> answer.when(eventService).addAttendeeToEvent(EventConstants.FIRST_EVENT_ID);
        }
    }

    private Object invoke(Operation operation) throws IOException {
        return switch (operation) {
            case FIRST_EVENT -> initializer.setupFirstEvent();
            case EVENT_FIRST -> initializer.setupEventByFirstUser();
            case EVENT_SECOND -> initializer.setupEventBySecondUser();
            case FILE -> initializer.setupFileInEvent(EventConstants.FIRST_EVENT_ID);
            case THREAD_FIRST -> initializer.setupThreadInEventByFirstUser(EventConstants.FIRST_EVENT_ID);
            case THREAD_SECOND -> initializer.setupThreadInEventBySecondUser(EventConstants.FIRST_EVENT_ID);
            case REPLY_FIRST -> initializer.setupThreadReplyInThreadByFirstUser(EventConstants.FIRST_EVENT_ID, ThreadConstants.FIRST_THREAD_ID);
            case REPLY_SECOND -> initializer.setupThreadReplyInThreadBySecondUser(EventConstants.FIRST_EVENT_ID, ThreadConstants.FIRST_THREAD_ID);
            case CUSTOM_REPLY_FIRST -> initializer.setupThreadReplyInThreadByFirstUser(EventConstants.FIRST_EVENT_ID, ThreadConstants.FIRST_THREAD_ID, ThreadReplyConstants.FIRST_REPLY_CONTENT + " custom");
            case CUSTOM_REPLY_SECOND -> initializer.setupThreadReplyInThreadBySecondUser(EventConstants.FIRST_EVENT_ID, ThreadConstants.FIRST_THREAD_ID, ThreadReplyConstants.FIRST_REPLY_CONTENT + " custom");
            case REPLIES_FIRST -> initializer.setupThreadRepliesInThreadByFirstUser(EventConstants.FIRST_EVENT_ID, ThreadConstants.FIRST_THREAD_ID, 2);
            case REPLIES_SECOND -> initializer.setupThreadRepliesInThreadBySecondUser(EventConstants.FIRST_EVENT_ID, ThreadConstants.FIRST_THREAD_ID, 2);
            case ATTENDANCE_FIRST -> { initializer.addFirstUserToAttendees(EventConstants.FIRST_EVENT_ID); yield null; }
            case ATTENDANCE_SECOND -> { initializer.addSecondUserToAttendees(EventConstants.FIRST_EVENT_ID); yield null; }
        };
    }

    private Object expectedResult(Operation operation) {
        return switch (operation) {
            case FIRST_EVENT, EVENT_FIRST, EVENT_SECOND -> EventConstants.FIRST_EVENT_ID;
            case THREAD_FIRST, THREAD_SECOND -> ThreadConstants.FIRST_THREAD_ID;
            case REPLY_FIRST, REPLY_SECOND, CUSTOM_REPLY_FIRST, CUSTOM_REPLY_SECOND -> ThreadReplyConstants.FIRST_REPLY_ID;
            case REPLIES_FIRST, REPLIES_SECOND -> List.of(ThreadReplyConstants.FIRST_REPLY_ID, ThreadReplyConstants.SECOND_REPLY_ID);
            case FILE -> FileConstants.JPG_FILE_ID;
            case ATTENDANCE_FIRST, ATTENDANCE_SECOND -> null;
        };
    }

    private void authenticate(String email) {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(email, null, RoleConstants.ROLE_USER_NAME));
    }

    private void assertActor(Operation operation) {
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo(actorEmail(operation));
    }

    private String actorEmail(Operation operation) {
        return isSecondUser(operation) ? UserConstants.SECOND_USER_EMAIL : UserConstants.FIRST_USER_EMAIL;
    }

    private boolean isSecondUser(Operation operation) {
        return switch (operation) {
            case EVENT_SECOND, THREAD_SECOND, REPLY_SECOND, CUSTOM_REPLY_SECOND, REPLIES_SECOND, ATTENDANCE_SECOND -> true;
            default -> false;
        };
    }

    // Test operation metadata, not a domain fixture or a new initializer framework.
    enum Operation {
        FIRST_EVENT, EVENT_FIRST, EVENT_SECOND, FILE, THREAD_FIRST, THREAD_SECOND,
        REPLY_FIRST, REPLY_SECOND, CUSTOM_REPLY_FIRST, CUSTOM_REPLY_SECOND,
        REPLIES_FIRST, REPLIES_SECOND, ATTENDANCE_FIRST, ATTENDANCE_SECOND
    }
}
