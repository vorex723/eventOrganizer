package com.mazurek.eventOrganizer.notification;

import com.mazurek.eventOrganizer.DeletionService;
import com.mazurek.eventOrganizer.auth.AuthenticationService;
import com.mazurek.eventOrganizer.auth.dto.AuthenticationRequest;
import com.mazurek.eventOrganizer.exception.common.InvalidPageNumberException;
import com.mazurek.eventOrganizer.exception.notification.NotificationNotFoundException;
import com.mazurek.eventOrganizer.exception.user.UserNotFoundException;
import com.mazurek.eventOrganizer.jwt.DeviceType;
import com.mazurek.eventOrganizer.notification.domain.Notification;
import com.mazurek.eventOrganizer.notification.domain.NotificationResourceType;
import com.mazurek.eventOrganizer.notification.dto.NotificationDto;
import com.mazurek.eventOrganizer.notification.dto.NotificationPageDto;
import com.mazurek.eventOrganizer.notification.repository.NotificationRepository;
import com.mazurek.eventOrganizer.testData.AuthHelper;
import com.mazurek.eventOrganizer.testData.builders.AuthenticationRequestTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.NotificationTestBuilder;
import com.mazurek.eventOrganizer.user.UserRepository;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static com.mazurek.eventOrganizer.testData.TestConstants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("NotificationController integration tests:")
public class NotificationControllerIntegrationTest {

    private final AuthenticationRequest firstUserAuthRequest =
            AuthenticationRequestTestBuilder.authenticationRequestForFirstUser().build();

    @Autowired
    private NotificationRepository notificationRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AuthenticationService authenticationService;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private AuthHelper authHelper;
    @Autowired
    private DeletionService deletionService;

    private UUID firstUserId;
    private UUID secondUserId;
    private String firstUserJwt;

    @BeforeEach
    void setUp() {
        deletionService.deleteAllSafe();
        authHelper.setupRolesAndUsers();

        firstUserId = getUserId(UserConstants.FIRST_USER_EMAIL);
        secondUserId = getUserId(UserConstants.SECOND_USER_EMAIL);
        firstUserJwt = generateJwt(firstUserAuthRequest);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        deletionService.deleteAllSafe();
    }

    @Nested
    @DisplayName("Get current user notifications tests: GET /api/v1/notifications")
    class GetCurrentUserNotificationsTests {

        @Test
        @DisplayName("When getting notifications should return HTTP 403 Forbidden if there is no Authorization header")
        public void whenGettingNotificationsShouldReturnHttpForbiddenIfThereIsNoAuthorizationHeader() throws Exception {
            getNotificationsWithoutAuth()
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("When getting notifications should return HTTP 403 Forbidden if Authorization header is empty")
        public void whenGettingNotificationsShouldReturnHttpForbiddenIfAuthorizationHeaderIsEmpty() throws Exception {
            getNotifications("", null)
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("When getting notifications should return HTTP 403 Forbidden if token is malformed")
        public void whenGettingNotificationsShouldReturnHttpForbiddenIfTokenIsMalformed() throws Exception {
            getNotifications(AuthConstants.JWT_PREFIX + "invalid-token", null)
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("When getting notifications should return HTTP 400 Bad Request if page is not a number")
        public void whenGettingNotificationsShouldReturnHttpBadRequestIfPageIsNotNumber() throws Exception {
            getNotifications(firstUserJwt, "not-a-number")
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("When getting notifications should return HTTP 400 Bad Request if page is negative")
        public void whenGettingNotificationsShouldReturnHttpBadRequestIfPageIsNegative() throws Exception {
            expectErrorJson(
                    getNotifications(firstUserJwt, String.valueOf(PaginationConstants.PAGE_MINUS_ONE)),
                    HttpStatus.BAD_REQUEST,
                    InvalidPageNumberException.DEFAULT_MESSAGE
            );
        }

        @Test
        @DisplayName("When getting notifications without page parameter should return an empty first page")
        public void whenGettingNotificationsWithoutPageParameterShouldReturnEmptyFirstPage() throws Exception {
            expectNotificationPageJson(
                    getNotifications(firstUserJwt, null),
                    0,
                    PaginationConstants.PAGE_ZERO,
                    0,
                    0,
                    true
            );
        }

        @Test
        @DisplayName("When getting notifications should return notification DTO with correct data")
        public void whenGettingNotificationsShouldReturnNotificationDtoWithCorrectData() throws Exception {
            Notification notification = persist(
                    NotificationTestBuilder.threadReplyNotification()
                            .readAt(NotificationConstants.THREAD_REPLY_NOTIFICATION_READ_AT),
                    firstUserId
            );

            expectNotificationPageJson(
                    getNotifications(firstUserJwt, String.valueOf(PaginationConstants.PAGE_ZERO)),
                    1,
                    PaginationConstants.PAGE_ZERO,
                    1,
                    1,
                    true
            )
                    .andExpect(jsonPath("$.notifications[0].id").value(notification.getId().toString()))
                    .andExpect(jsonPath("$.notifications[0].title").value(notification.getTitle()))
                    .andExpect(jsonPath("$.notifications[0].body").value(notification.getBody()))
                    .andExpect(jsonPath("$.notifications[0].resourceType").value(NotificationResourceType.THREAD.name()))
                    .andExpect(jsonPath("$.notifications[0].resourceId").value(notification.getResourceId().toString()))
                    .andExpect(jsonPath("$.notifications[0].parentResourceType").value(NotificationResourceType.EVENT.name()))
                    .andExpect(jsonPath("$.notifications[0].parentResourceId").value(notification.getParentResourceId().toString()))
                    .andExpect(jsonPath("$.notifications[0].createdAt").value(notification.getCreatedAt().toString()))
                    .andExpect(jsonPath("$.notifications[0].readAt").value(notification.getReadAt().toString()))
                    .andExpect(jsonPath("$.notifications[0].read").value(true));
        }

        @Test
        @DisplayName("When getting notifications should return only current user's notifications")
        public void whenGettingNotificationsShouldReturnOnlyCurrentUserNotifications() throws Exception {
            Notification currentUserNotification = persist(
                    NotificationTestBuilder.privateMessageNotification(),
                    firstUserId
            );
            persist(NotificationTestBuilder.threadReplyNotification(), secondUserId);

            expectNotificationPageJson(
                    getNotifications(firstUserJwt, String.valueOf(PaginationConstants.PAGE_ZERO)),
                    1,
                    PaginationConstants.PAGE_ZERO,
                    1,
                    1,
                    true
            )
                    .andExpect(jsonPath("$.notifications[0].id").value(currentUserNotification.getId().toString()))
                    .andExpect(jsonPath("$.notifications[0].readAt").doesNotExist())
                    .andExpect(jsonPath("$.notifications[0].read").value(false));
        }

        @Test
        @DisplayName("When getting notifications should return newest notifications first with stable id order")
        public void whenGettingNotificationsShouldReturnNewestNotificationsFirstWithStableIdOrder() throws Exception {
            Notification olderNotification = persist(
                    NotificationTestBuilder.privateMessageNotification()
                            .createdAt(TimeConstants.ONE_HOUR_AGO),
                    firstUserId
            );
            Notification firstSameTimeNotification = persist(
                    NotificationTestBuilder.threadReplyNotification()
                            .createdAt(TimeConstants.NOW),
                    firstUserId
            );
            Notification secondSameTimeNotification = persist(
                    NotificationTestBuilder.eventUpdateNotification()
                            .createdAt(TimeConstants.NOW),
                    firstUserId
            );

            NotificationPageDto response = readNotificationPage(
                    getNotifications(firstUserJwt, String.valueOf(PaginationConstants.PAGE_ZERO))
                            .andExpect(status().isOk())
                            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                            .andReturn()
            );
            List<UUID> sameTimeIds = List.of(firstSameTimeNotification, secondSameTimeNotification).stream()
                    .sorted(Comparator.comparing(
                            (Notification notification) -> notification.getId().toString()
                    ).reversed())
                    .map(Notification::getId)
                    .toList();

            assertThat(response.notifications())
                    .extracting(NotificationDto::id)
                    .containsExactly(sameTimeIds.get(0), sameTimeIds.get(1), olderNotification.getId());
        }

        @Test
        @DisplayName("When getting notifications should respect pagination across multiple pages")
        public void whenGettingNotificationsShouldRespectPaginationAcrossMultiplePages() throws Exception {
            List<Notification> savedNotifications = IntStream.range(
                            0,
                            PaginationConstants.DEFAULT_PAGE_SIZE + 1
                    )
                    .mapToObj(index -> persist(
                            NotificationTestBuilder.privateMessageNotification()
                                    .createdAt(TimeConstants.NOW.minusSeconds(index)),
                            firstUserId
                    ))
                    .toList();

            NotificationPageDto firstPage = readNotificationPage(
                    getNotifications(firstUserJwt, String.valueOf(PaginationConstants.PAGE_ZERO))
                            .andExpect(status().isOk())
                            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                            .andReturn()
            );
            NotificationPageDto secondPage = readNotificationPage(
                    getNotifications(firstUserJwt, String.valueOf(PaginationConstants.PAGE_ONE))
                            .andExpect(status().isOk())
                            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                            .andReturn()
            );

            assertPageMetadata(
                    firstPage,
                    PaginationConstants.DEFAULT_PAGE_SIZE,
                    PaginationConstants.PAGE_ZERO,
                    PaginationConstants.DEFAULT_PAGE_SIZE + 1L,
                    2,
                    false
            );
            assertPageMetadata(
                    secondPage,
                    1,
                    PaginationConstants.PAGE_ONE,
                    PaginationConstants.DEFAULT_PAGE_SIZE + 1L,
                    2,
                    true
            );

            assertThat(firstPage.notifications())
                    .extracting(NotificationDto::id)
                    .containsExactlyElementsOf(
                            savedNotifications.subList(0, PaginationConstants.DEFAULT_PAGE_SIZE).stream()
                                    .map(Notification::getId)
                                    .toList()
                    );
            assertThat(secondPage.notifications())
                    .extracting(NotificationDto::id)
                    .containsExactly(savedNotifications.getLast().getId());
        }
    }

    @Nested
    @DisplayName("Get current user unread count tests: GET /api/v1/notifications/unread-count")
    class GetCurrentUserUnreadCountTests {

        @Test
        @DisplayName("When getting unread count should return HTTP 403 Forbidden if there is no Authorization header")
        public void whenGettingUnreadCountShouldReturnHttpForbiddenIfThereIsNoAuthorizationHeader() throws Exception {
            getUnreadCountWithoutAuth()
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("When getting unread count should return HTTP 403 Forbidden if Authorization header is empty")
        public void whenGettingUnreadCountShouldReturnHttpForbiddenIfAuthorizationHeaderIsEmpty() throws Exception {
            getUnreadCount("")
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("When getting unread count should return HTTP 403 Forbidden if token is malformed")
        public void whenGettingUnreadCountShouldReturnHttpForbiddenIfTokenIsMalformed() throws Exception {
            getUnreadCount(AuthConstants.JWT_PREFIX + "invalid-token")
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("When getting unread count should return zero if user has no notifications")
        public void whenGettingUnreadCountShouldReturnZeroIfUserHasNoNotifications() throws Exception {
            getUnreadCount(firstUserJwt)
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.unreadCount").value(0));
        }

        @Test
        @DisplayName("When getting unread count should count only current user's unread notifications")
        public void whenGettingUnreadCountShouldCountOnlyCurrentUserUnreadNotifications() throws Exception {
            persist(NotificationTestBuilder.privateMessageNotification(), firstUserId);
            persist(NotificationTestBuilder.threadReplyNotification(), firstUserId);
            persist(
                    NotificationTestBuilder.eventUpdateNotification()
                            .readAt(NotificationConstants.PRIVATE_MESSAGE_NOTIFICATION_READ_AT),
                    firstUserId
            );
            persist(NotificationTestBuilder.newEventFileNotification(), secondUserId);

            getUnreadCount(firstUserJwt)
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.unreadCount").value(2));
        }
    }

    @Nested
    @DisplayName("Mark notification as read tests: PATCH /api/v1/notifications/{notificationId}/read")
    class MarkNotificationAsReadTests {

        @Test
        @DisplayName("When marking notification as read should return HTTP 403 Forbidden if there is no Authorization header")
        public void whenMarkingNotificationAsReadShouldReturnHttpForbiddenIfThereIsNoAuthorizationHeader() throws Exception {
            markNotificationAsReadWithoutAuth(NotificationConstants.NOT_EXISTING_NOTIFICATION_ID)
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("When marking notification as read should return HTTP 403 Forbidden if Authorization header is empty")
        public void whenMarkingNotificationAsReadShouldReturnHttpForbiddenIfAuthorizationHeaderIsEmpty() throws Exception {
            markNotificationAsRead("", NotificationConstants.NOT_EXISTING_NOTIFICATION_ID)
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("When marking notification as read should return HTTP 403 Forbidden if token is malformed")
        public void whenMarkingNotificationAsReadShouldReturnHttpForbiddenIfTokenIsMalformed() throws Exception {
            markNotificationAsRead(
                    AuthConstants.JWT_PREFIX + "invalid-token",
                    NotificationConstants.NOT_EXISTING_NOTIFICATION_ID
            )
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("When marking notification as read should return HTTP 400 Bad Request if id is malformed")
        public void whenMarkingNotificationAsReadShouldReturnHttpBadRequestIfIdIsMalformed() throws Exception {
            markNotificationAsRead(firstUserJwt, "not-a-uuid")
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("When marking notification as read should return HTTP 404 Not Found if notification does not exist")
        public void whenMarkingNotificationAsReadShouldReturnHttpNotFoundIfNotificationDoesNotExist() throws Exception {
            expectErrorJson(
                    markNotificationAsRead(firstUserJwt, NotificationConstants.NOT_EXISTING_NOTIFICATION_ID),
                    HttpStatus.NOT_FOUND,
                    NotificationNotFoundException.DEFAULT_MESSAGE
            );
        }

        @Test
        @DisplayName("When marking another user's notification as read should return HTTP 404 Not Found and leave it unread")
        public void whenMarkingAnotherUserNotificationAsReadShouldReturnHttpNotFoundAndLeaveItUnread() throws Exception {
            Notification anotherUserNotification = persist(
                    NotificationTestBuilder.privateMessageNotification(),
                    secondUserId
            );

            expectErrorJson(
                    markNotificationAsRead(firstUserJwt, anotherUserNotification.getId()),
                    HttpStatus.NOT_FOUND,
                    NotificationNotFoundException.DEFAULT_MESSAGE
            );

            assertThat(getNotification(anotherUserNotification.getId()).getReadAt()).isNull();
        }

        @Test
        @DisplayName("When marking owned unread notification as read should return HTTP 204 No Content and persist read time")
        public void whenMarkingOwnedUnreadNotificationAsReadShouldReturnHttpNoContentAndPersistReadTime() throws Exception {
            Notification unreadNotification = persist(
                    NotificationTestBuilder.privateMessageNotification(),
                    firstUserId
            );

            markNotificationAsRead(firstUserJwt, unreadNotification.getId())
                    .andExpect(status().isNoContent())
                    .andExpect(content().string(""));

            assertThat(getNotification(unreadNotification.getId()).getReadAt())
                    .isEqualTo(TimeConstants.NOW);
        }

        @Test
        @DisplayName("When marking already-read notification as read should return HTTP 204 No Content and preserve read time")
        public void whenMarkingAlreadyReadNotificationAsReadShouldReturnHttpNoContentAndPreserveReadTime() throws Exception {
            Instant originalReadAt = NotificationConstants.THREAD_REPLY_NOTIFICATION_READ_AT;
            Notification alreadyReadNotification = persist(
                    NotificationTestBuilder.threadReplyNotification().readAt(originalReadAt),
                    firstUserId
            );

            markNotificationAsRead(firstUserJwt, alreadyReadNotification.getId())
                    .andExpect(status().isNoContent())
                    .andExpect(content().string(""));

            assertThat(getNotification(alreadyReadNotification.getId()).getReadAt())
                    .isEqualTo(originalReadAt);
        }
    }

    @Nested
    @DisplayName("Mark all notifications as read tests: PATCH /api/v1/notifications/read-all")
    class MarkAllNotificationsAsReadTests {

        @Test
        @DisplayName("When marking all notifications as read should return HTTP 403 Forbidden if there is no Authorization header")
        public void whenMarkingAllNotificationsAsReadShouldReturnHttpForbiddenIfThereIsNoAuthorizationHeader() throws Exception {
            markAllNotificationsAsReadWithoutAuth()
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("When marking all notifications as read should return HTTP 403 Forbidden if Authorization header is empty")
        public void whenMarkingAllNotificationsAsReadShouldReturnHttpForbiddenIfAuthorizationHeaderIsEmpty() throws Exception {
            markAllNotificationsAsRead("")
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("When marking all notifications as read should return HTTP 403 Forbidden if token is malformed")
        public void whenMarkingAllNotificationsAsReadShouldReturnHttpForbiddenIfTokenIsMalformed() throws Exception {
            markAllNotificationsAsRead(AuthConstants.JWT_PREFIX + "invalid-token")
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("When marking all notifications as read with no notifications should return HTTP 204 No Content")
        public void whenMarkingAllNotificationsAsReadWithNoNotificationsShouldReturnHttpNoContent() throws Exception {
            markAllNotificationsAsRead(firstUserJwt)
                    .andExpect(status().isNoContent())
                    .andExpect(content().string(""));
        }

        @Test
        @DisplayName("When marking all notifications as read should update only current user's unread notifications")
        public void whenMarkingAllNotificationsAsReadShouldUpdateOnlyCurrentUserUnreadNotifications() throws Exception {
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

            markAllNotificationsAsRead(firstUserJwt)
                    .andExpect(status().isNoContent())
                    .andExpect(content().string(""));

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(getNotification(firstUnreadNotification.getId()).getReadAt())
                        .isEqualTo(TimeConstants.NOW);
                softly.assertThat(getNotification(secondUnreadNotification.getId()).getReadAt())
                        .isEqualTo(TimeConstants.NOW);
                softly.assertThat(getNotification(alreadyReadNotification.getId()).getReadAt())
                        .isEqualTo(originalReadAt);
                softly.assertThat(getNotification(anotherUserUnreadNotification.getId()).getReadAt())
                        .isNull();
            });
        }
    }

    private UUID getUserId(String email) {
        return userRepository.findByIgnoreCaseEmail(email)
                .orElseThrow(UserNotFoundException::new)
                .getId();
    }

    private String generateJwt(AuthenticationRequest authenticationRequest) {
        return AuthConstants.JWT_PREFIX +
                authenticationService.authenticate(authenticationRequest, DeviceType.WEB).getAccessToken();
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

    private ResultActions getNotifications(String jwt, String pageNumber) throws Exception {
        var requestBuilder = get(ApiConstants.NOTIFICATIONS_URL)
                .header(ApiConstants.AUTHORIZATION_HEADER, jwt);

        if (pageNumber != null) {
            requestBuilder.param("page", pageNumber);
        }

        return mockMvc.perform(requestBuilder);
    }

    private ResultActions getNotificationsWithoutAuth() throws Exception {
        return mockMvc.perform(get(ApiConstants.NOTIFICATIONS_URL));
    }

    private ResultActions getUnreadCount(String jwt) throws Exception {
        return mockMvc.perform(
                get(ApiConstants.NOTIFICATIONS_UNREAD_COUNT_URL)
                        .header(ApiConstants.AUTHORIZATION_HEADER, jwt)
        );
    }

    private ResultActions getUnreadCountWithoutAuth() throws Exception {
        return mockMvc.perform(get(ApiConstants.NOTIFICATIONS_UNREAD_COUNT_URL));
    }

    private ResultActions markNotificationAsRead(String jwt, Object notificationId) throws Exception {
        return mockMvc.perform(
                patch(ApiConstants.NOTIFICATION_READ_URL, notificationId)
                        .header(ApiConstants.AUTHORIZATION_HEADER, jwt)
        );
    }

    private ResultActions markNotificationAsReadWithoutAuth(Object notificationId) throws Exception {
        return mockMvc.perform(patch(ApiConstants.NOTIFICATION_READ_URL, notificationId));
    }

    private ResultActions markAllNotificationsAsRead(String jwt) throws Exception {
        return mockMvc.perform(
                patch(ApiConstants.NOTIFICATIONS_READ_ALL_URL)
                        .header(ApiConstants.AUTHORIZATION_HEADER, jwt)
        );
    }

    private ResultActions markAllNotificationsAsReadWithoutAuth() throws Exception {
        return mockMvc.perform(patch(ApiConstants.NOTIFICATIONS_READ_ALL_URL));
    }

    private ResultActions expectErrorJson(
            ResultActions resultActions,
            HttpStatus expectedStatus,
            String expectedMessage
    ) throws Exception {
        return resultActions
                .andExpect(status().is(expectedStatus.value()))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(expectedStatus.value()))
                .andExpect(jsonPath("$.message").value(expectedMessage));
    }

    private ResultActions expectNotificationPageJson(
            ResultActions resultActions,
            int notificationsLength,
            int pageNumber,
            long totalElements,
            int totalPages,
            boolean lastPage
    ) throws Exception {
        return resultActions
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.notifications.length()").value(notificationsLength))
                .andExpect(jsonPath("$.pageNumber").value(pageNumber))
                .andExpect(jsonPath("$.pageSize").value(PaginationConstants.DEFAULT_PAGE_SIZE))
                .andExpect(jsonPath("$.totalElements").value(totalElements))
                .andExpect(jsonPath("$.totalPages").value(totalPages))
                .andExpect(jsonPath("$.lastPage").value(lastPage));
    }

    private NotificationPageDto readNotificationPage(MvcResult mvcResult) throws Exception {
        return objectMapper.readValue(
                mvcResult.getResponse().getContentAsString(),
                NotificationPageDto.class
        );
    }

    private void assertPageMetadata(
            NotificationPageDto page,
            int expectedNotificationsLength,
            int expectedPageNumber,
            long expectedTotalElements,
            int expectedTotalPages,
            boolean expectedLastPage
    ) {
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(page.notifications()).hasSize(expectedNotificationsLength);
            softly.assertThat(page.pageNumber()).isEqualTo(expectedPageNumber);
            softly.assertThat(page.pageSize()).isEqualTo(PaginationConstants.DEFAULT_PAGE_SIZE);
            softly.assertThat(page.totalElements()).isEqualTo(expectedTotalElements);
            softly.assertThat(page.totalPages()).isEqualTo(expectedTotalPages);
            softly.assertThat(page.lastPage()).isEqualTo(expectedLastPage);
        });
    }
}
