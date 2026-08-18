package com.mazurek.eventOrganizer.notification.service;

import com.mazurek.eventOrganizer.DeletionService;
import com.mazurek.eventOrganizer.exception.notification.NotificationNotFoundException;
import com.mazurek.eventOrganizer.exception.user.UserNotFoundException;
import com.mazurek.eventOrganizer.notification.domain.Notification;
import com.mazurek.eventOrganizer.notification.dto.NotificationDto;
import com.mazurek.eventOrganizer.notification.dto.NotificationPageDto;
import com.mazurek.eventOrganizer.notification.dto.NotificationUnreadCountDto;
import com.mazurek.eventOrganizer.notification.repository.NotificationRepository;
import com.mazurek.eventOrganizer.testData.AuthHelper;
import com.mazurek.eventOrganizer.testData.builders.NotificationTestBuilder;
import com.mazurek.eventOrganizer.user.UserRepository;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static com.mazurek.eventOrganizer.testData.TestConstants.NotificationConstants;
import static com.mazurek.eventOrganizer.testData.TestConstants.TimeConstants;
import static com.mazurek.eventOrganizer.testData.TestConstants.UserConstants;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = "app.pagination.default-page-size=3")
@ActiveProfiles("test")
@DisplayName("NotificationQueryService integration tests:")
class NotificationQueryServiceImplIntegrationTest {

    @Autowired
    private NotificationQueryService notificationQueryService;
    @Autowired
    private NotificationRepository notificationRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AuthHelper authHelper;
    @Autowired
    private DeletionService deletionService;

    private UUID firstUserId;
    private UUID secondUserId;

    @BeforeEach
    void setUp() {
        deletionService.deleteAllSafe();
        authHelper.setupRolesAndUsers();

        firstUserId = getUserId(UserConstants.FIRST_USER_EMAIL);
        secondUserId = getUserId(UserConstants.SECOND_USER_EMAIL);
        authHelper.setupSecurityContextForFirstUser();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        deletionService.deleteAllSafe();
    }

    @Test
    @DisplayName("When getting notifications should return only current user pages in newest-first order")
    void whenGettingNotificationsShouldReturnOnlyCurrentUserPagesInNewestFirstOrder() {
        Notification oldestNotification = persist(
                NotificationTestBuilder.privateMessageNotification(),
                firstUserId
        );
        Notification readNotification = persist(
                NotificationTestBuilder.threadReplyNotification()
                        .readAt(NotificationConstants.THREAD_REPLY_NOTIFICATION_READ_AT),
                firstUserId
        );
        Notification firstSameTimeNotification = persist(
                NotificationTestBuilder.eventUpdateNotification()
                        .createdAt(NotificationConstants.NEW_EVENT_FILE_NOTIFICATION_CREATED_AT),
                firstUserId
        );
        Notification secondSameTimeNotification = persist(
                NotificationTestBuilder.newEventFileNotification(),
                firstUserId
        );
        Notification newestNotification = persist(
                NotificationTestBuilder.newEventThreadNotification(),
                firstUserId
        );
        persist(
                NotificationTestBuilder.privateMessageNotification()
                        .createdAt(TimeConstants.NOW.plusSeconds(1)),
                secondUserId
        );

        NotificationPageDto firstPage = notificationQueryService.getCurrentUserNotifications(0);
        NotificationPageDto secondPage = notificationQueryService.getCurrentUserNotifications(1);
        List<Notification> sameTimeNotificationsNewestFirst = List.of(
                        firstSameTimeNotification,
                        secondSameTimeNotification
                ).stream()
                .sorted(Comparator.comparing(
                        (Notification notification) -> notification.getId().toString()
                ).reversed())
                .toList();

        assertThat(firstPage.notifications())
                .extracting(NotificationDto::id)
                .containsExactly(
                        newestNotification.getId(),
                        sameTimeNotificationsNewestFirst.get(0).getId(),
                        sameTimeNotificationsNewestFirst.get(1).getId()
                );
        assertThat(secondPage.notifications())
                .extracting(NotificationDto::id)
                .containsExactly(readNotification.getId(), oldestNotification.getId());

        assertPageMetadata(firstPage, 0, 5, 2, false);
        assertPageMetadata(secondPage, 1, 5, 2, true);

        assertNotificationDto(newestNotification, firstPage.notifications().get(0));
        assertNotificationDto(sameTimeNotificationsNewestFirst.get(0), firstPage.notifications().get(1));
        assertNotificationDto(sameTimeNotificationsNewestFirst.get(1), firstPage.notifications().get(2));
        assertNotificationDto(readNotification, secondPage.notifications().get(0));
        assertNotificationDto(oldestNotification, secondPage.notifications().get(1));
    }

    @Test
    @DisplayName("When getting unread count should count only current user's unread notifications")
    void whenGettingUnreadCountShouldCountOnlyCurrentUserUnreadNotifications() {
        persist(NotificationTestBuilder.privateMessageNotification(), firstUserId);
        persist(
                NotificationTestBuilder.threadReplyNotification()
                        .readAt(NotificationConstants.THREAD_REPLY_NOTIFICATION_READ_AT),
                firstUserId
        );
        persist(NotificationTestBuilder.eventUpdateNotification(), firstUserId);
        persist(NotificationTestBuilder.newEventFileNotification(), secondUserId);

        NotificationUnreadCountDto result = notificationQueryService.getCurrentUserUnreadCount();

        assertThat(result.unreadCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("When marking owned unread notification should persist read time through dirty checking")
    void whenMarkingOwnedUnreadNotificationShouldPersistReadTimeThroughDirtyChecking() {
        Notification unreadNotification = persist(
                NotificationTestBuilder.privateMessageNotification(),
                firstUserId
        );

        notificationQueryService.markAsRead(unreadNotification.getId());

        assertThat(getNotification(unreadNotification.getId()).getReadAt())
                .isEqualTo(TimeConstants.NOW);
    }

    @Test
    @DisplayName("When marking already-read notification should preserve original read time")
    void whenMarkingAlreadyReadNotificationShouldPreserveOriginalReadTime() {
        Instant originalReadAt = NotificationConstants.THREAD_REPLY_NOTIFICATION_READ_AT;
        Notification alreadyReadNotification = persist(
                NotificationTestBuilder.threadReplyNotification().readAt(originalReadAt),
                firstUserId
        );

        notificationQueryService.markAsRead(alreadyReadNotification.getId());

        assertThat(getNotification(alreadyReadNotification.getId()).getReadAt())
                .isEqualTo(originalReadAt);
    }

    @Test
    @DisplayName("When marking another user's notification should respond as not found and leave it unread")
    void whenMarkingAnotherUserNotificationShouldRespondAsNotFoundAndLeaveItUnread() {
        Notification anotherUserNotification = persist(
                NotificationTestBuilder.privateMessageNotification(),
                secondUserId
        );

        assertThatThrownBy(() -> notificationQueryService.markAsRead(anotherUserNotification.getId()))
                .isInstanceOf(NotificationNotFoundException.class);

        assertThat(getNotification(anotherUserNotification.getId()).getReadAt()).isNull();
    }

    @Test
    @DisplayName("When marking all as read should update only current user's unread notifications")
    void whenMarkingAllAsReadShouldUpdateOnlyCurrentUserUnreadNotifications() {
        Notification firstUnreadNotification = persist(
                NotificationTestBuilder.privateMessageNotification(),
                firstUserId
        );
        Notification secondUnreadNotification = persist(
                NotificationTestBuilder.threadReplyNotification(),
                firstUserId
        );
        Instant originalReadAt = NotificationConstants.PRIVATE_MESSAGE_NOTIFICATION_READ_AT;
        Notification alreadyReadNotification = persist(
                NotificationTestBuilder.eventUpdateNotification().readAt(originalReadAt),
                firstUserId
        );
        Notification anotherUserUnreadNotification = persist(
                NotificationTestBuilder.newEventFileNotification(),
                secondUserId
        );

        notificationQueryService.markAllAsRead();

        Notification reloadedFirstUnread = getNotification(firstUnreadNotification.getId());
        Notification reloadedSecondUnread = getNotification(secondUnreadNotification.getId());
        Notification reloadedAlreadyRead = getNotification(alreadyReadNotification.getId());
        Notification reloadedAnotherUserUnread = getNotification(anotherUserUnreadNotification.getId());

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(reloadedFirstUnread.getReadAt()).isEqualTo(TimeConstants.NOW);
            softly.assertThat(reloadedSecondUnread.getReadAt()).isEqualTo(TimeConstants.NOW);
            softly.assertThat(reloadedAlreadyRead.getReadAt()).isEqualTo(originalReadAt);
            softly.assertThat(reloadedAnotherUserUnread.getReadAt()).isNull();
        });
    }

    private UUID getUserId(String email) {
        return userRepository.findByIgnoreCaseEmail(email)
                .orElseThrow(UserNotFoundException::new)
                .getId();
    }

    private Notification persist(NotificationTestBuilder builder, UUID recipientId) {
        return notificationRepository.saveAndFlush(
                builder
                        .id(null)
                        .recipientId(recipientId)
                        .build()
        );
    }

    private Notification getNotification(UUID notificationId) {
        return notificationRepository.findById(notificationId).orElseThrow();
    }

    private void assertPageMetadata(
            NotificationPageDto page,
            int expectedPageNumber,
            long expectedTotalElements,
            int expectedTotalPages,
            boolean expectedLastPage
    ) {
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(page.pageNumber()).isEqualTo(expectedPageNumber);
            softly.assertThat(page.pageSize()).isEqualTo(3);
            softly.assertThat(page.totalElements()).isEqualTo(expectedTotalElements);
            softly.assertThat(page.totalPages()).isEqualTo(expectedTotalPages);
            softly.assertThat(page.lastPage()).isEqualTo(expectedLastPage);
        });
    }

    private void assertNotificationDto(Notification notification, NotificationDto dto) {
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(dto.id()).isEqualTo(notification.getId());
            softly.assertThat(dto.title()).isEqualTo(notification.getTitle());
            softly.assertThat(dto.body()).isEqualTo(notification.getBody());
            softly.assertThat(dto.resourceType()).isEqualTo(notification.getResourceType());
            softly.assertThat(dto.resourceId()).isEqualTo(notification.getResourceId());
            softly.assertThat(dto.parentResourceType()).isEqualTo(notification.getParentResourceType());
            softly.assertThat(dto.parentResourceId()).isEqualTo(notification.getParentResourceId());
            softly.assertThat(dto.createdAt()).isEqualTo(notification.getCreatedAt());
            softly.assertThat(dto.readAt()).isEqualTo(notification.getReadAt());
            softly.assertThat(dto.read()).isEqualTo(notification.isRead());
        });
    }
}
