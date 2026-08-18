package com.mazurek.eventOrganizer.notification.service;

import com.mazurek.eventOrganizer.auth.AuthenticationService;
import com.mazurek.eventOrganizer.config.properties.PaginationProperties;
import com.mazurek.eventOrganizer.exception.common.InvalidPageNumberException;
import com.mazurek.eventOrganizer.exception.notification.NotificationNotFoundException;
import com.mazurek.eventOrganizer.notification.domain.Notification;
import com.mazurek.eventOrganizer.notification.domain.NotificationResourceType;
import com.mazurek.eventOrganizer.notification.dto.NotificationDto;
import com.mazurek.eventOrganizer.notification.dto.NotificationPageDto;
import com.mazurek.eventOrganizer.notification.dto.NotificationUnreadCountDto;
import com.mazurek.eventOrganizer.notification.repository.NotificationRepository;
import com.mazurek.eventOrganizer.testData.builders.NotificationTestBuilder;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.mazurek.eventOrganizer.testData.TestConstants.EventConstants;
import static com.mazurek.eventOrganizer.testData.TestConstants.NotificationConstants;
import static com.mazurek.eventOrganizer.testData.TestConstants.PaginationConstants;
import static com.mazurek.eventOrganizer.testData.TestConstants.TimeConstants;
import static com.mazurek.eventOrganizer.testData.TestConstants.UserConstants;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationQueryServiceImpl unit tests:")
class NotificationQueryServiceImplUnitTest {

    @Mock
    private Clock clock;
    @Mock
    private AuthenticationService authenticationService;
    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private PaginationProperties paginationProperties;
    @InjectMocks
    private NotificationQueryServiceImpl notificationQueryService;

    @Nested
    @DisplayName("Get current user notifications tests:")
    class GetCurrentUserNotificationsTests {

        private static final int EXISTING_NOTIFICATION_COUNT = 4;

        private final UUID currentUserId = UserConstants.FIRST_USER_ID;

        private Notification newEventFileNotification;
        private Notification eventUpdateNotification;
        private Notification readNotification;
        private Notification oldestUnreadNotification;
        private PageRequest firstPageRequest;
        private Page<Notification> populatedFirstPage;
        private Page<Notification> emptyFirstPage;
        private Page<Notification> emptyLaterPage;

        @BeforeEach
        void setUp() {
            newEventFileNotification = NotificationTestBuilder.newEventFileNotification().build();
            eventUpdateNotification = NotificationTestBuilder.eventUpdateNotification().build();
            readNotification = NotificationTestBuilder.threadReplyNotification()
                    .readAt(NotificationConstants.THREAD_REPLY_NOTIFICATION_READ_AT)
                    .build();
            oldestUnreadNotification = NotificationTestBuilder.privateMessageNotification().build();

            Sort newestFirst = Sort.by(Sort.Direction.DESC, "createdAt", "id");
            firstPageRequest = PageRequest.of(
                    PaginationConstants.PAGE_ZERO,
                    PaginationConstants.DEFAULT_PAGE_SIZE,
                    newestFirst
            );
            PageRequest laterPageRequest = PageRequest.of(
                    PaginationConstants.PAGE_ONE,
                    PaginationConstants.DEFAULT_PAGE_SIZE,
                    newestFirst
            );

            populatedFirstPage = new PageImpl<>(
                    List.of(
                            newEventFileNotification,
                            eventUpdateNotification,
                            readNotification,
                            oldestUnreadNotification
                    ),
                    firstPageRequest,
                    PaginationConstants.THIRTY_ELEMENTS
            );
            emptyFirstPage = new PageImpl<>(List.of(), firstPageRequest, 0);
            emptyLaterPage = new PageImpl<>(
                    List.of(),
                    laterPageRequest,
                    EXISTING_NOTIFICATION_COUNT
            );
        }

        @Test
        @DisplayName("When page number is negative should throw InvalidPageNumberException")
        void whenPageNumberIsNegativeShouldThrowInvalidPageNumberException() {
            assertThatThrownBy(() -> notificationQueryService.getCurrentUserNotifications(
                    PaginationConstants.PAGE_MINUS_ONE
            )).isInstanceOf(InvalidPageNumberException.class);

            verifyNoInteractions(authenticationService, paginationProperties, notificationRepository);
        }

        @Test
        @DisplayName("When getting notifications should load notifications for current user")
        void whenGettingNotificationsShouldLoadNotificationsForCurrentUser() {
            setupSuccessfulMocks(populatedFirstPage);

            notificationQueryService.getCurrentUserNotifications(PaginationConstants.PAGE_ZERO);

            verify(authenticationService, times(1)).getCurrentUserId();
            verify(notificationRepository, times(1))
                    .findByRecipientId(eq(currentUserId), any(Pageable.class));
        }

        @Test
        @DisplayName("When getting notifications should use requested page, configured size and newest-first sort")
        void whenGettingNotificationsShouldUseCorrectPageRequest() {
            setupSuccessfulMocks(populatedFirstPage);

            notificationQueryService.getCurrentUserNotifications(PaginationConstants.PAGE_ZERO);

            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
            verify(notificationRepository, times(1))
                    .findByRecipientId(eq(currentUserId), pageableCaptor.capture());

            Pageable pageable = pageableCaptor.getValue();
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(pageable.getPageNumber()).isEqualTo(PaginationConstants.PAGE_ZERO);
                softly.assertThat(pageable.getPageSize()).isEqualTo(PaginationConstants.DEFAULT_PAGE_SIZE);
                softly.assertThat(pageable.getSort())
                        .isEqualTo(Sort.by(Sort.Direction.DESC, "createdAt", "id"));
            });
            verify(paginationProperties, times(1)).getDefaultPageSize();
        }

        @Test
        @DisplayName("When getting notifications should map notifications and preserve repository order")
        void whenGettingNotificationsShouldMapNotificationsAndPreserveRepositoryOrder() {
            setupSuccessfulMocks(populatedFirstPage);

            NotificationPageDto result = notificationQueryService
                    .getCurrentUserNotifications(PaginationConstants.PAGE_ZERO);

            assertThat(result.notifications())
                    .extracting(NotificationDto::id)
                    .containsExactly(
                            newEventFileNotification.getId(),
                            eventUpdateNotification.getId(),
                            readNotification.getId(),
                            oldestUnreadNotification.getId()
                    );
            assertNotificationDto(newEventFileNotification, result.notifications().get(0));
            assertNotificationDto(eventUpdateNotification, result.notifications().get(1));
            assertNotificationDto(readNotification, result.notifications().get(2));
            assertNotificationDto(oldestUnreadNotification, result.notifications().get(3));

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(result.notifications().get(0).parentResourceType())
                        .isEqualTo(NotificationResourceType.EVENT);
                softly.assertThat(result.notifications().get(0).parentResourceId())
                        .isEqualTo(EventConstants.FIRST_EVENT_ID);
                softly.assertThat(result.notifications().get(1).parentResourceType()).isNull();
                softly.assertThat(result.notifications().get(1).parentResourceId()).isNull();
                softly.assertThat(result.notifications().get(2).parentResourceType())
                        .isEqualTo(NotificationResourceType.EVENT);
                softly.assertThat(result.notifications().get(2).parentResourceId())
                        .isEqualTo(EventConstants.FIRST_EVENT_ID);
                softly.assertThat(result.notifications().get(3).parentResourceType()).isNull();
                softly.assertThat(result.notifications().get(3).parentResourceId()).isNull();
            });
        }

        @Test
        @DisplayName("When getting notifications should map page metadata")
        void whenGettingNotificationsShouldMapPageMetadata() {
            setupSuccessfulMocks(populatedFirstPage);

            NotificationPageDto result = notificationQueryService
                    .getCurrentUserNotifications(PaginationConstants.PAGE_ZERO);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(result.pageNumber()).isEqualTo(PaginationConstants.PAGE_ZERO);
                softly.assertThat(result.pageSize()).isEqualTo(PaginationConstants.DEFAULT_PAGE_SIZE);
                softly.assertThat(result.totalElements()).isEqualTo(PaginationConstants.THIRTY_ELEMENTS);
                softly.assertThat(result.totalPages()).isEqualTo(2);
                softly.assertThat(result.lastPage()).isFalse();
            });
        }

        @Test
        @DisplayName("When current user has no notifications should return empty first page")
        void whenCurrentUserHasNoNotificationsShouldReturnEmptyFirstPage() {
            setupSuccessfulMocks(emptyFirstPage);

            NotificationPageDto result = notificationQueryService
                    .getCurrentUserNotifications(PaginationConstants.PAGE_ZERO);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(result.notifications()).isEmpty();
                softly.assertThat(result.pageNumber()).isEqualTo(PaginationConstants.PAGE_ZERO);
                softly.assertThat(result.pageSize()).isEqualTo(PaginationConstants.DEFAULT_PAGE_SIZE);
                softly.assertThat(result.totalElements()).isZero();
                softly.assertThat(result.totalPages()).isZero();
                softly.assertThat(result.lastPage()).isTrue();
            });
        }

        @Test
        @DisplayName("When requested later page has no content should return its empty page metadata")
        void whenRequestedLaterPageHasNoContentShouldReturnItsEmptyPageMetadata() {
            setupSuccessfulMocks(emptyLaterPage);

            NotificationPageDto result = notificationQueryService
                    .getCurrentUserNotifications(PaginationConstants.PAGE_ONE);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(result.notifications()).isEmpty();
                softly.assertThat(result.pageNumber()).isEqualTo(PaginationConstants.PAGE_ONE);
                softly.assertThat(result.pageSize()).isEqualTo(PaginationConstants.DEFAULT_PAGE_SIZE);
                softly.assertThat(result.totalElements()).isEqualTo(EXISTING_NOTIFICATION_COUNT);
                softly.assertThat(result.totalPages()).isEqualTo(1);
                softly.assertThat(result.lastPage()).isTrue();
            });
        }

        private void setupSuccessfulMocks(Page<Notification> page) {
            when(authenticationService.getCurrentUserId()).thenReturn(currentUserId);
            when(paginationProperties.getDefaultPageSize())
                    .thenReturn(PaginationConstants.DEFAULT_PAGE_SIZE);
            when(notificationRepository.findByRecipientId(eq(currentUserId), any(Pageable.class)))
                    .thenReturn(page);
        }

        private void assertNotificationDto(Notification notification, NotificationDto notificationDto) {
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(notificationDto.id()).isEqualTo(notification.getId());
                softly.assertThat(notificationDto.title()).isEqualTo(notification.getTitle());
                softly.assertThat(notificationDto.body()).isEqualTo(notification.getBody());
                softly.assertThat(notificationDto.resourceType()).isEqualTo(notification.getResourceType());
                softly.assertThat(notificationDto.resourceId()).isEqualTo(notification.getResourceId());
                softly.assertThat(notificationDto.parentResourceType())
                        .isEqualTo(notification.getParentResourceType());
                softly.assertThat(notificationDto.parentResourceId())
                        .isEqualTo(notification.getParentResourceId());
                softly.assertThat(notificationDto.createdAt()).isEqualTo(notification.getCreatedAt());
                softly.assertThat(notificationDto.readAt()).isEqualTo(notification.getReadAt());
                softly.assertThat(notificationDto.read()).isEqualTo(notification.isRead());
            });
        }
    }

    @Nested
    @DisplayName("Get current user unread count tests:")
    class GetCurrentUserUnreadCountTests {

        private static final long UNREAD_NOTIFICATION_COUNT = 3L;

        @Test
        @DisplayName("When getting unread count should count unread notifications for current user")
        void whenGettingUnreadCountShouldCountUnreadNotificationsForCurrentUser() {
            when(authenticationService.getCurrentUserId()).thenReturn(UserConstants.FIRST_USER_ID);
            when(notificationRepository.countByRecipientIdAndReadAtIsNull(UserConstants.FIRST_USER_ID))
                    .thenReturn(UNREAD_NOTIFICATION_COUNT);

            NotificationUnreadCountDto result = notificationQueryService.getCurrentUserUnreadCount();

            assertThat(result.unreadCount()).isEqualTo(UNREAD_NOTIFICATION_COUNT);
            verify(authenticationService, times(1)).getCurrentUserId();
            verify(notificationRepository, times(1))
                    .countByRecipientIdAndReadAtIsNull(UserConstants.FIRST_USER_ID);
        }

        @Test
        @DisplayName("When current user has no unread notifications should return zero")
        void whenCurrentUserHasNoUnreadNotificationsShouldReturnZero() {
            when(authenticationService.getCurrentUserId()).thenReturn(UserConstants.FIRST_USER_ID);
            when(notificationRepository.countByRecipientIdAndReadAtIsNull(UserConstants.FIRST_USER_ID))
                    .thenReturn(0L);

            NotificationUnreadCountDto result = notificationQueryService.getCurrentUserUnreadCount();

            assertThat(result.unreadCount()).isZero();
        }
    }

    @Nested
    @DisplayName("Mark notification as read tests:")
    class MarkAsReadTests {

        private final UUID currentUserId = UserConstants.FIRST_USER_ID;

        private Notification unreadNotification;
        private Notification alreadyReadNotification;

        @BeforeEach
        void setUp() {
            unreadNotification = NotificationTestBuilder.privateMessageNotification().build();
            alreadyReadNotification = NotificationTestBuilder.threadReplyNotification()
                    .readAt(NotificationConstants.THREAD_REPLY_NOTIFICATION_READ_AT)
                    .build();
        }

        @Test
        @DisplayName("When marking owned unread notification should set read time")
        void whenMarkingOwnedUnreadNotificationShouldSetReadTime() {
            setupOwnedNotification(unreadNotification);
            Notification expectedNotification = NotificationTestBuilder.privateMessageNotification()
                    .readAt(TimeConstants.NOW)
                    .build();

            notificationQueryService.markAsRead(unreadNotification.getId());

            assertThat(unreadNotification)
                    .usingRecursiveComparison()
                    .isEqualTo(expectedNotification);
            verify(notificationRepository, never()).save(any(Notification.class));
        }

        @Test
        @DisplayName("When marking notification should look it up by requested id and current user id")
        void whenMarkingNotificationShouldLookItUpByRequestedIdAndCurrentUserId() {
            setupOwnedNotification(unreadNotification);

            notificationQueryService.markAsRead(unreadNotification.getId());

            ArgumentCaptor<UUID> notificationIdCaptor = ArgumentCaptor.forClass(UUID.class);
            ArgumentCaptor<UUID> recipientIdCaptor = ArgumentCaptor.forClass(UUID.class);
            verify(notificationRepository, times(1)).findByIdAndRecipientId(
                    notificationIdCaptor.capture(),
                    recipientIdCaptor.capture()
            );

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(notificationIdCaptor.getValue()).isEqualTo(unreadNotification.getId());
                softly.assertThat(recipientIdCaptor.getValue()).isEqualTo(currentUserId);
            });
        }

        @Test
        @DisplayName("When notification is already read should preserve original read time")
        void whenNotificationIsAlreadyReadShouldPreserveOriginalReadTime() {
            setupOwnedNotification(alreadyReadNotification);

            notificationQueryService.markAsRead(alreadyReadNotification.getId());

            assertThat(alreadyReadNotification.getReadAt())
                    .isEqualTo(NotificationConstants.THREAD_REPLY_NOTIFICATION_READ_AT);
            verify(notificationRepository, never()).save(any(Notification.class));
        }

        @Test
        @DisplayName("When notification does not exist should throw NotificationNotFoundException")
        void whenNotificationDoesNotExistShouldThrowNotificationNotFoundException() {
            when(authenticationService.getCurrentUserId()).thenReturn(currentUserId);
            when(notificationRepository.findByIdAndRecipientId(
                    NotificationConstants.NOT_EXISTING_NOTIFICATION_ID,
                    currentUserId
            )).thenReturn(Optional.empty());

            assertThatThrownBy(() -> notificationQueryService.markAsRead(
                    NotificationConstants.NOT_EXISTING_NOTIFICATION_ID
            )).isInstanceOf(NotificationNotFoundException.class);

            verifyNoInteractions(clock);
            verify(notificationRepository, never()).save(any(Notification.class));
        }

        @Test
        @DisplayName("When notification belongs to another user should respond as not found")
        void whenNotificationBelongsToAnotherUserShouldRespondAsNotFound() {
            Notification anotherUserNotification = NotificationTestBuilder.privateMessageNotification()
                    .recipientId(UserConstants.SECOND_USER_ID)
                    .build();
            when(authenticationService.getCurrentUserId()).thenReturn(currentUserId);
            when(notificationRepository.findByIdAndRecipientId(
                    anotherUserNotification.getId(),
                    currentUserId
            )).thenReturn(Optional.empty());

            assertThatThrownBy(() -> notificationQueryService.markAsRead(anotherUserNotification.getId()))
                    .isInstanceOf(NotificationNotFoundException.class);

            verifyNoInteractions(clock);
            verify(notificationRepository, never()).save(any(Notification.class));
        }

        private void setupOwnedNotification(Notification notification) {
            when(authenticationService.getCurrentUserId()).thenReturn(currentUserId);
            when(notificationRepository.findByIdAndRecipientId(notification.getId(), currentUserId))
                    .thenReturn(Optional.of(notification));
            when(clock.instant()).thenReturn(TimeConstants.NOW);
        }
    }

    @Nested
    @DisplayName("Mark all notifications as read tests:")
    class MarkAllAsReadTests {

        private static final int UPDATED_NOTIFICATION_COUNT = 3;

        private final UUID currentUserId = UserConstants.FIRST_USER_ID;

        @Test
        @DisplayName("When marking all notifications as read should update unread notifications for current user")
        void whenMarkingAllNotificationsAsReadShouldUpdateUnreadNotificationsForCurrentUser() {
            setupCurrentUserAndClock();
            when(notificationRepository.markAllAsRead(currentUserId, TimeConstants.NOW))
                    .thenReturn(UPDATED_NOTIFICATION_COUNT);

            notificationQueryService.markAllAsRead();

            verify(authenticationService, times(1)).getCurrentUserId();
            verify(clock, times(1)).instant();
            verify(notificationRepository, times(1)).markAllAsRead(currentUserId, TimeConstants.NOW);
            verify(notificationRepository, never()).save(any(Notification.class));
            verify(notificationRepository, never()).findByIdAndRecipientId(any(UUID.class), any(UUID.class));
        }

        @Test
        @DisplayName("When current user has no unread notifications should complete without an exception")
        void whenCurrentUserHasNoUnreadNotificationsShouldCompleteWithoutException() {
            setupCurrentUserAndClock();
            when(notificationRepository.markAllAsRead(currentUserId, TimeConstants.NOW)).thenReturn(0);

            assertThatCode(notificationQueryService::markAllAsRead)
                    .doesNotThrowAnyException();

            verify(notificationRepository, times(1)).markAllAsRead(currentUserId, TimeConstants.NOW);
        }

        private void setupCurrentUserAndClock() {
            when(authenticationService.getCurrentUserId()).thenReturn(currentUserId);
            when(clock.instant()).thenReturn(TimeConstants.NOW);
        }
    }
}
