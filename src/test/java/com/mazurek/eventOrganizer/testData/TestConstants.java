package com.mazurek.eventOrganizer.testData;

import com.mazurek.eventOrganizer.jwt.DeviceType;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.UUID;

public class TestConstants {
    /**
     * Central location for all test constants.
     * Use these constants across all tests to ensure consistency.
     * <p>
     * Naming Convention:
     * - FIRST_* = Primary test entity (owner, creator)
     * - SECOND_* = Secondary test entity (attendee, participant)
     * - THIRD_* = Tertiary test entity (unauthorized user)
     * - OLD_* = Entity beyond time restrictions
     * - PAST_* = Entity with dates in the past
     * - FUTURE_* = Entity with dates in the future
     */

    private TestConstants() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static class AuthConstants{
        public static final String JWT_PREFIX = "Bearer ";
        public static final String ACTIVATION_RESULT_BASE_URL = "http://localhost:8080/activation-result";
        public static final String REGISTRATION_VERIFICATION_REQUIRED_MESSAGE = "Verify your email to get access.";
    }

    public static class DeviceConstants {
        public static final String USER_AGENT_HEADER = "User-Agent";
        public static final String DEVICE_TYPE_HEADER = "X-Device-Type";
        public static final String INVALID_DEVICE_TYPE_HEADER = "INVALID_DEVICE_TYPE";
        public static final String USER_AGENT_ANDROID_MOBILE =
                "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36";
        public static final String USER_AGENT_ANDROID_TABLET =
                "Mozilla/5.0 (Linux; Android 13; SM-X700) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36";
        public static final String USER_AGENT_IPHONE =
                "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1";
        public static final String USER_AGENT_IPAD =
                "Mozilla/5.0 (iPad; CPU OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1";
        public static final String USER_AGENT_DESKTOP_WINDOWS =
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36";
        public static final String USER_AGENT_UNKNOWN = "CustomClient/1.0";
    }

    public static class UserConstants {

        // First User (Primary test user - event owner, thread creator)
        public static final UUID FIRST_USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
        public static final String FIRST_USER_EMAIL = "first.user@example.com";
        public static final String FIRST_USER_FIRST_NAME = "First";
        public static final String FIRST_USER_LAST_NAME = "User";
        public static final String FIRST_USER_FULL_NAME = "First User";
        public static final String USER_PASSWORD = "passwo0rD#";
        public static final String WRONG_USER_PASSWORD = "WrongPasswo0rD#";
        public static final String NEW_PASSWORD = "NewPassword1@";
        public static final String FIRST_USER_TIMEZONE = "Europe/Warsaw";
        public static final String FIRST_USER_FCM_TOKEN = "fcm-token-first-user";
        public static final String FIRST_USER_NEW_FCM_TOKEN = "fcm-token-first-user-new";
        public static final String FIRST_USER_NEW_EMAIL = "first.user.updated@example.com";
        public static final String INVALID_FIRST_NAME = "a";
        public static final String INVALID_LAST_NAME = "b";
        public static final String INVALID_CITY_NAME = "x";
        // Second User (Secondary test user - event attendee)
        public static final UUID SECOND_USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
        public static final String SECOND_USER_EMAIL = "second.user@example.com";
        public static final String SECOND_USER_FIRST_NAME = "Second";
        public static final String SECOND_USER_LAST_NAME = "User";
        public static final String SECOND_USER_FULL_NAME = "Second User";
        // Third User (Used for unauthorized access tests)
        public static final UUID THIRD_USER_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
        public static final String THIRD_USER_EMAIL = "third.user@example.com";
        public static final String THIRD_USER_FIRST_NAME = "Third";
        public static final String THIRD_USER_LAST_NAME = "User";
        public static final String THIRD_USER_FULL_NAME = "Third User";
        public static final String NOT_EXISTING_USER_EMAIL = "missing.user@example.com";
        public static final UUID NOT_EXISTING_USER_ID = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");
        // Admin User
        public static final UUID ADMIN_USER_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        public static final String ADMIN_USER_EMAIL = "admin@example.com";
        public static final String ADMIN_USER_FIRST_NAME = "Admin";
        public static final String ADMIN_USER_LAST_NAME = "User";
        // Deleted User Placeholder
        public static final UUID DELETED_USER_ID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
        public static final String DELETED_USER_EMAIL = "deleted.user@system.internal";
        public static final String DELETED_USER_FIRST_NAME = "Deleted";
        public static final String DELETED_USER_LAST_NAME = "User";
    }

    public static class CitiesConstants {

        public static final UUID WARSAW_ID = UUID.fromString("c1111111-1111-1111-1111-111111111111");
        public static final String WARSAW_NAME = "warsaw";
        public static final UUID KRAKOW_ID = UUID.fromString("c2222222-2222-2222-2222-222222222222");
        public static final String KRAKOW_NAME = "krakow";
        public static final UUID SYSTEM_CITY_ID = UUID.fromString("c0000000-0000-0000-0000-000000000000");
        public static final String SYSTEM_CITY_NAME = "system";
    }

    public static class EventConstants {

        public static final UUID FIRST_EVENT_ID = UUID.fromString("e1111111-1111-1111-1111-111111111111");
        public static final String FIRST_EVENT_NAME = "First Event";
        public static final String FIRST_EVENT_SHORT_DESC = "First event short description";
        public static final String FIRST_EVENT_LONG_DESC = "First event long description ".repeat(20).trim();
        public static final String EVENT_UPDATE_LONG_DESCRIPTION = "First updated long description ".repeat(20).trim();
        public static final String FIRST_EVENT_ADDRESS = "Main Street 1, Warsaw";
        public static final UUID NOT_EXISTING_EVENT_ID = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");
        public static final String EVENT_UPDATE_NAME = "FIRST EVENT UPDATED";
        public static final String EVENT_UPDATE_SHORT_DESCRIPTION = "UPDATE FIRST event short description";
        public static final String EVENT_UPDATE_CITY = CitiesConstants.KRAKOW_NAME;
        public static final String EVENT_UPDATE_EXACT_ADDRESS = "Ul. Kosciuszki 2";


        public static final UUID SECOND_EVENT_ID = UUID.fromString("e2222222-2222-2222-2222-222222222222");
        public static final String SECOND_EVENT_NAME = "Second Event";
        public static final String SECOND_EVENT_SHORT_DESC = "Second event short description";
        public static final String SECOND_EVENT_LONG_DESC = "Second event long description";
        public static final String SECOND_EVENT_ADDRESS = "Second Street 2, Krakow";
        // Past event (for testing event completion logic)
        public static final UUID PAST_EVENT_ID = UUID.fromString("e9999999-9999-9999-9999-999999999999");
        public static final String PAST_EVENT_NAME = "Past Event";

        public static final String WRONG_NAME = "abcd";
        public static final String WRONG_EXACT_ADDRESS = "wrong address".repeat(10).trim();
        public static final String WRONG_SHORT_DESCRIPTION_TOO_SHORT = "to short";
        public static final String WRONG_SHORT_DESCRIPTION_TOO_LONG = "Short description too long".repeat(20).trim();
        public static final String WRONG_LONG_DESCRIPTION_TOO_SHORT = "too short";
        public static final String WRONG_TAG_NAME = " ";


    }

    public static class ThreadConstants {

        public static final UUID FIRST_THREAD_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
        public static final String FIRST_THREAD_NAME = "First Thread";
        public static final String FIRST_THREAD_CONTENT = "This is the first thread content";
        public static final String FIRST_THREAD_NAME_UPDATE = "First Thread Update";
        public static final String FIRST_THREAD_CONTENT_UPDATE = "This is the first thread content Update";
        public static final UUID SECOND_THREAD_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
        public static final String SECOND_THREAD_NAME = "Second Thread";
        public static final String SECOND_THREAD_CONTENT = "This is the second thread content";
        public static final UUID THIRD_THREAD_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
        public static final UUID NOT_EXISTING_THREAD_ID = UUID.fromString("99999999-1111-1111-1111-111111111111");
        // Old thread (for testing time-based deletion restrictions)
        public static final UUID OLD_THREAD_ID = UUID.fromString("99999999-9999-9999-9999-999999999999");
        public static final String OLD_THREAD_NAME = "Old Thread";
        public static final int INITIAL_EDIT_COUNTER = 0;
        public static final boolean THREAD_EXISTS_IN_EVENT = true;
        public static final boolean THREAD_DOES_NOT_EXIST_IN_EVENT = false;
    }


    public static class ThreadReplyConstants {

        public static final UUID FIRST_REPLY_ID = UUID.fromString("aaaaaaaa-1111-1111-1111-111111111111");
        public static final String FIRST_REPLY_CONTENT = "This is the first reply";
        public static final String FIRST_REPLY_UPDATE_CONTENT = "This is the first reply";
        public static final String CONTROLLER_THREAD_REPLY_CONTENT = "test thread reply message";
        public static final String CONTROLLER_THREAD_REPLY_CONTENT_UPDATE = "updated test thread reply message";
        public static final UUID SECOND_REPLY_ID = UUID.fromString("aaaaaaaa-2222-2222-2222-222222222222");
        public static final String SECOND_REPLY_CONTENT = "This is the second reply";
        public static final UUID THIRD_REPLY_ID = UUID.fromString("aaaaaaaa-3333-3333-3333-333333333333");
        public static final String THIRD_REPLY_CONTENT = "This is the third reply";
        public static final UUID NOT_EXISTING_REPLY_ID = UUID.fromString("aaaaaaaa-4444-4444-4444-444444444444");
        // Old reply
        public static final UUID OLD_REPLY_ID = UUID.fromString("aaaaaaaa-9999-9999-9999-999999999999");
        public static final String OLD_REPLY_CONTENT = "This reply is too old to be edited";
        public static final int INITIAL_EDIT_COUNTER = 0;
    }

    public static class FileConstants {

        // Allowed image types
        public static final UUID JPG_FILE_ID = UUID.fromString("f1111111-1111-1111-1111-111111111111");
        // Backward compatibility aliases
        public static final UUID FIRST_FILE_ID = JPG_FILE_ID;
        public static final String JPG_FILE_ORIGINAL_NAME = "test-photo.jpg";
        public static final String FIRST_FILE_ORIGINAL_NAME = JPG_FILE_ORIGINAL_NAME;
        public static final String JPG_FILE_CONTENT_TYPE = "image/jpeg";
        public static final String FIRST_FILE_CONTENT_TYPE = JPG_FILE_CONTENT_TYPE;
        public static final UUID JPEG_FILE_ID = UUID.fromString("f1111112-1111-1111-1111-111111111112");
        public static final String JPEG_FILE_ORIGINAL_NAME = "test-photo.jpeg";
        public static final String JPEG_FILE_CONTENT_TYPE = "image/jpeg";
        public static final UUID PNG_FILE_ID = UUID.fromString("f2222222-2222-2222-2222-222222222222");
        public static final UUID SECOND_FILE_ID = PNG_FILE_ID;
        public static final String PNG_FILE_ORIGINAL_NAME = "test-image.png";
        public static final String SECOND_FILE_ORIGINAL_NAME = PNG_FILE_ORIGINAL_NAME;
        public static final String PNG_FILE_CONTENT_TYPE = "image/png";
        // Allowed document types
        public static final UUID PDF_FILE_ID = UUID.fromString("f3333333-3333-3333-3333-333333333333");
        public static final UUID THIRD_FILE_ID = PDF_FILE_ID;
        public static final String PDF_FILE_ORIGINAL_NAME = "test-document.pdf";
        public static final String THIRD_FILE_ORIGINAL_NAME = PDF_FILE_ORIGINAL_NAME;
        public static final String PDF_FILE_CONTENT_TYPE = "application/pdf";
        public static final UUID DOC_FILE_ID = UUID.fromString("f4444444-4444-4444-4444-444444444444");
        public static final String DOC_FILE_ORIGINAL_NAME = "test-document.doc";
        public static final String DOC_FILE_CONTENT_TYPE = "application/msword";
        public static final UUID DOCX_FILE_ID = UUID.fromString("f4444445-4444-4444-4444-444444444445");
        public static final String DOCX_FILE_ORIGINAL_NAME = "test-document.docx";
        public static final String DOCX_FILE_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        public static final UUID ODT_FILE_ID = UUID.fromString("f4444446-4444-4444-4444-444444444446");
        public static final String ODT_FILE_ORIGINAL_NAME = "test-document.odt";
        public static final String ODT_FILE_CONTENT_TYPE = "application/vnd.oasis.opendocument.text";
        // Allowed presentation types
        public static final UUID PPT_FILE_ID = UUID.fromString("f5555555-5555-5555-5555-555555555555");
        public static final String PPT_FILE_ORIGINAL_NAME = "test-presentation.ppt";
        public static final String PPT_FILE_CONTENT_TYPE = "application/vnd.ms-powerpoint";
        public static final UUID PPTX_FILE_ID = UUID.fromString("f5555556-5555-5555-5555-555555555556");
        public static final String PPTX_FILE_ORIGINAL_NAME = "test-presentation.pptx";
        public static final String PPTX_FILE_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.presentationml.presentation";
        // Allowed spreadsheet types
        public static final UUID XLS_FILE_ID = UUID.fromString("f6666666-6666-6666-6666-666666666666");
        public static final String XLS_FILE_ORIGINAL_NAME = "test-spreadsheet.xls";
        public static final String XLS_FILE_CONTENT_TYPE = "application/vnd.ms-excel";
        public static final UUID XLSX_FILE_ID = UUID.fromString("f6666667-6666-6666-6666-666666666667");
        public static final String XLSX_FILE_ORIGINAL_NAME = "test-spreadsheet.xlsx";
        public static final String XLSX_FILE_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        // Allowed video types
        public static final UUID MP4_FILE_ID = UUID.fromString("f7777777-7777-7777-7777-777777777777");
        public static final String MP4_FILE_ORIGINAL_NAME = "test-video.mp4";
        public static final String MP4_FILE_CONTENT_TYPE = "video/mp4";
        public static final UUID AVI_FILE_ID = UUID.fromString("f7777778-7777-7777-7777-777777777778");
        public static final String AVI_FILE_ORIGINAL_NAME = "test-video.avi";
        public static final String AVI_FILE_CONTENT_TYPE = "video/x-msvideo";
        public static final String FIRST_FILE_USER_NAME = "unique-file-name-123.jpg";
        public static final String USER_FILE_NAME = "User File Name";
        public static final String SECOND_FILE_USER_NAME = "unique-file-name-456.png";
        public static final String THIRD_FILE_USER_NAME = "unique-file-name-789.pdf";
        public static final UUID NOT_EXISTING_FILE_ID = UUID.fromString("f9999999-9999-9999-9999-999999999999");
        //malware file
        public static final String MALWARE_FILE_USER_FILE_NAME = "not malware file";
        public static final String MALWARE_FILE_ORIGINAL_NAME = "malware.zip";
        public static final String MALWARE_FILE_CONTENT_TYPE =  "application/zip";
        public static final String FILE_MULTIPART_PART_NAME = "file";
        public static final String WRONG_USER_FILE_NAME_TOO_LONG = "file-name".repeat(40);
    }

    public static class ConversationConstants {

        public static final UUID FIRST_CONVERSATION_ID = UUID.fromString("cccccccc-1111-1111-1111-111111111111");
        public static final UUID SECOND_CONVERSATION_ID = UUID.fromString("cccccccc-2222-2222-2222-222222222222");
    }

    public static class MessageConstants {

        public static final UUID FIRST_MESSAGE_ID = UUID.fromString("bbbbbbbb-1111-1111-1111-111111111111");
        public static final String FIRST_MESSAGE_CONTENT = "Hello, this is the first message";
        public static final UUID SECOND_MESSAGE_ID = UUID.fromString("bbbbbbbb-2222-2222-2222-222222222222");
        public static final String SECOND_MESSAGE_CONTENT = "This is a reply to the first message";
        public static final UUID THIRD_MESSAGE_ID = UUID.fromString("bbbbbbbb-3333-3333-3333-333333333333");
        public static final String THIRD_MESSAGE_CONTENT = "This is another message in the conversation";
    }


    public static class TagConstants {

        public static final UUID FIRST_TAG_ID = UUID.fromString("dddddddd-1111-1111-1111-111111111111");
        public static final String FIRST_TAG_NAME = "technology";
        public static final UUID SECOND_TAG_ID = UUID.fromString("dddddddd-2222-2222-2222-222222222222");
        public static final String SECOND_TAG_NAME = "sports";
        public static final UUID THIRD_TAG_ID = UUID.fromString("dddddddd-3333-3333-3333-333333333333");
        public static final String THIRD_TAG_NAME = "culture";
        public static final String FOURTH_TAG_NAME = "web-dev";
        public static final Set<String> DEFAULT_EVENT_TAGS = Set.of(FIRST_TAG_NAME, SECOND_TAG_NAME);
        public static final Set<String> EVENT_UPDATE_TAGS = Set.of(THIRD_TAG_NAME, SECOND_TAG_NAME);
        public static final Set<String> REPLACEMENT_EVENT_TAGS = Set.of(FOURTH_TAG_NAME, "ai");
    }


    public static class RoleConstants {

        public static final Long USER_ROLE_ID = 1L;
        public static final String ROLE_USER_NAME = "ROLE_USER";
        public static final Long ADMIN_ROLE_ID = 2L;
        public static final String ROLE_ADMIN_NAME = "ROLE_ADMIN";
    }

    public static class ActivationTokenConstants {
        public static final Long FIRST_ACTIVATION_TOKEN_ID = 1L;
        public static final Long SECOND_ACTIVATION_TOKEN_ID = 2L;
        public static final UUID FIRST_ACTIVATION_TOKEN_UUID = UUID.fromString("dededede-dede-dede-dede-dededededede");
        public static final UUID SECOND_ACTIVATION_TOKEN_UUID = UUID.fromString("efefefef-efef-efef-efef-efefefefefef");
        public static final long ACTIVATION_TOKEN_EXPIRATION_SECONDS = 345_600_000L;
    }

    public static class RefreshTokenConstants {
        public static final Long FIRST_REFRESH_TOKEN_ID = 1L;
        public static final Long SECOND_REFRESH_TOKEN_ID = 2L;
        public static final Long THIRD_REFRESH_TOKEN_ID = 3L;

        public static final String FIRST_REFRESH_TOKEN = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1";
        public static final String SECOND_REFRESH_TOKEN = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb2";
        public static final String EXPIRED_REFRESH_TOKEN = "cccccccc-cccc-cccc-cccc-ccccccccccc3";
        public static final String REVOKED_REFRESH_TOKEN = "dddddddd-dddd-dddd-dddd-ddddddddddd4";
        public static final String NOT_EXISTING_REFRESH_TOKEN = "eeeeeeee-eeee-eeee-eeee-eeeeeeeeeee5";

        public static final DeviceType FIRST_REFRESH_TOKEN_DEVICE_TYPE = DeviceType.WEB;
        public static final DeviceType SECOND_REFRESH_TOKEN_DEVICE_TYPE = DeviceType.MOBILE_ANDROID;
        public static final DeviceType THIRD_REFRESH_TOKEN_DEVICE_TYPE = DeviceType.DESKTOP;

        public static final String FIRST_REFRESH_TOKEN_DEVICE_INFO = DeviceConstants.USER_AGENT_DESKTOP_WINDOWS;
        public static final String SECOND_REFRESH_TOKEN_DEVICE_INFO = DeviceConstants.USER_AGENT_ANDROID_MOBILE;
        public static final String THIRD_REFRESH_TOKEN_DEVICE_INFO = DeviceConstants.USER_AGENT_UNKNOWN;

        public static final boolean REFRESH_TOKEN_REVOKED_FALSE = false;
        public static final boolean REFRESH_TOKEN_REVOKED_TRUE = true;

        public static final Instant FIRST_REFRESH_TOKEN_CREATED_AT = TimeConstants.TWO_HOURS_AGO;
        public static final Instant FIRST_REFRESH_TOKEN_LAST_USED_AT = TimeConstants.NOW.minus(1, ChronoUnit.HOURS);
        public static final Instant FIRST_REFRESH_TOKEN_EXPIRY_DATE = TimeConstants.NOW.plus(1, ChronoUnit.DAYS);

        public static final Instant EXPIRED_REFRESH_TOKEN_CREATED_AT = TimeConstants.ONE_WEEK_AGO;
        public static final Instant EXPIRED_REFRESH_TOKEN_LAST_USED_AT = TimeConstants.ONE_HOUR_AGO;
        public static final Instant EXPIRED_REFRESH_TOKEN_EXPIRY_DATE = TimeConstants.ONE_HOUR_AGO;
    }

    public static class JwtConstants {
        public static final String TEST_SECRET_BASE64 = "dGVzdC1zZWNyZXQtdGhhdC1pcy1sb25nLWVub3VnaC1mb3ItSFMyNTY=";
        public static final String DIFFERENT_TEST_SECRET_BASE64 = "ZGlmZmVyZW50LXNlY3JldC10aGF0LWlzLWxvbmctZW5vdWdoLWZvci1IUzI1Ng==";
        public static final String ACCESS_TOKEN = "11111111-2222-3333-4444-555555555555";
        public static final String MALFORMED_TOKEN = "this.is.not.a.valid.jwt.token";
        public static final long TOKEN_ID_ONE = 1L;
        public static final long TOKEN_ID_TWO = 2L;
        public static final long ACCESS_TOKEN_EXPIRATION_30_SECONDS = 30_000L;
        public static final long ACCESS_TOKEN_EXPIRATION_30_MINUTES = 1_800_000L;
        public static final long REFRESH_TOKEN_EXPIRATION_SHORT = 86_400_000L;
        public static final long REFRESH_TOKEN_EXPIRATION_LONG = 2_592_000_000L;

    }

    public static class TimeConstants {

        public static final Instant NOW = Instant.parse("2026-04-08T12:00:00Z");
        public static final Clock FIXED_CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
        public static final LocalDateTime LOCAL_DATE_TIME_NOW = LocalDateTime.ofInstant(NOW, ZoneOffset.UTC);
        public static final Instant ONE_WEEK_FROM_NOW = NOW.plus(7, ChronoUnit.DAYS);
        public static final Instant EVENT_UPDATE_START_DATE = NOW.plus(30, ChronoUnit.DAYS);
        public static final Instant TWO_DAYS_FROM_NOW = NOW.plus(2, ChronoUnit.DAYS);
        public static final Instant TWO_DAYS_AGO = NOW.minus(2, ChronoUnit.DAYS);
        public static final Instant ONE_WEEK_AGO = NOW.minus(7, ChronoUnit.DAYS);
        public static final Instant SEVEN_HOURS_AGO = NOW.minus(7, ChronoUnit.HOURS);
        public static final Instant ONE_HOUR_AGO = NOW.minus(1, ChronoUnit.HOURS);
        public static final Instant TWO_HOURS_AGO = NOW.minus(2, ChronoUnit.HOURS);
        public static final Instant ONE_MONTH_FROM_NOW = NOW.plus(30, ChronoUnit.DAYS);
        public static final Instant ONE_MONTH_AGO = NOW.minus(30, ChronoUnit.DAYS);
    }

    public static class ApiConstants {
        public static final String AUTHORIZATION_HEADER = "Authorization";
        public static final String AUTH_REGISTER_URL = "/api/v1/auth/register";
        public static final String AUTH_LOGIN_URL = "/api/v1/auth/login";
        public static final String AUTH_LOGOUT_URL = "/api/v1/auth/logout";
        public static final String AUTH_REFRESH_URL = "/api/v1/auth/refresh";
        public static final String AUTH_ACTIVATE_RESEND_URL = "/api/v1/auth/activate";
        public static final String AUTH_ACTIVATE_URL = "/api/v1/auth/activate/{tokenId}";
        public static final String EVENTS_URL = "/api/v1/events";
        public static final String EVENT_BY_ID_URL = "/api/v1/events/{eventId}";
        public static final String EVENT_ATTEND_URL = "/api/v1/events/{eventId}/attend";
        public static final String EVENT_FILES_URL = "/api/v1/events/{eventId}/files";
        public static final String EVENT_FILE_BY_ID_URL = "/api/v1/events/{eventId}/files/{fileId}";
        public static final String EVENT_FILE_DATA_URL = "/api/v1/events/{eventId}/files/{fileId}/data";
        public static final String EVENT_THREADS_URL = "/api/v1/events/{eventId}/threads";
        public static final String EVENT_THREAD_BY_ID_URL = "/api/v1/events/{eventId}/threads/{threadId}";
        public static final String EVENT_THREAD_REPLIES_URL = "/api/v1/events/{eventId}/threads/{threadId}/replies";
        public static final String EVENT_THREAD_REPLY_BY_ID_URL = "/api/v1/events/{eventId}/threads/{threadId}/replies/{replyId}";
        public static final String USER_BY_ID_URL = "/api/v1/users/{id}";
        public static final String USER_EVENTS_URL = "/api/v1/users/{id}/events";
        public static final String USER_ATTENDING_EVENTS_URL = "/api/v1/users/me/attending-events";
        public static final String USER_REGISTER_FCM_TOKEN_URL = "/api/v1/users/register-token";
        public static final String USER_UPDATE_DETAILS_URL = "/api/v1/users/update";
        public static final String USER_CHANGE_PASSWORD_URL = "/api/v1/users/change-password";
        public static final String USER_CHANGE_EMAIL_URL = "/api/v1/users/change-email";
        public static final String CITY_BY_NAME_URL = "/api/v1/cities/{cityName}";
        public static final String TAG_BY_NAME_URL = "/api/v1/tags/{tagName}";
    }

    public static class PaginationConstants {
        public static final int PAGE_MINUS_ONE = -1;
        public static final int PAGE_ZERO = 0;
        public static final int PAGE_ONE = 1;
        public static final int FIVE_ELEMENTS = 5;
        public static final int TEN_ELEMENTS = 10;
        public static final int TWENTY_ELEMENTS = 20;
        public static final int THIRTY_ELEMENTS = 30;
        public static final int EVENT_PAGE_SIZE = 20;
        public static final int FILE_PAGE_SIZE = 20;
    }

    public static class ValidationConstants {
        public static final int THREAD_EDIT_WINDOW_HOURS = 6;
    }

    public static class InvalidInputConstants {
        public static final String BLANK_VALUE = " ";
        public static final String EMPTY_VALUE = "";
        public static final String INVALID_EMAIL = "not-an-email";
        public static final String INVALID_SHORT_PASSWORD = "sh";
        public static final String WEAK_PASSWORD = "weak";
        public static final String DIFFERENT_EMAIL = "different@example.com";
        public static final String DIFFERENT_PASSWORD = "DifferentP@ss1";
        public static final String WRONG_LOGIN_PASSWORD = "WrongP@ss1";
        public static final String DUPLICATE_KEY_MESSAGE = "duplicate key";
        public static final String INVALID_TIME_ZONE = "Invalid/TimeZone";
    }

    public static class ErrorConstants {
        public static final String SENSITIVE_RUNTIME_MESSAGE = "sensitive internal runtime details";
        public static final String SENSITIVE_ROLE_MESSAGE = "role lookup failed in private bootstrap path";
    }
}
