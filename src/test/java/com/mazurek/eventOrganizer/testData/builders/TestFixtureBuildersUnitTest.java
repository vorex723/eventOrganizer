package com.mazurek.eventOrganizer.testData.builders;

import com.mazurek.eventOrganizer.notification.firebaseCloudMessaging.FcmSendOutcome;
import com.mazurek.eventOrganizer.testData.TestFileContentFactory;
import com.mazurek.eventOrganizer.testData.builders.dto.*;
import com.mazurek.eventOrganizer.validators.MinFutureDateOffsetValidator;
import com.mazurek.eventOrganizer.validators.ValidEventCapacityValidator;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorFactory;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.time.Instant;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static com.mazurek.eventOrganizer.testData.TestConstants.*;
import static com.mazurek.eventOrganizer.testData.TestFailureHelper.requirePresent;
import static org.assertj.core.api.Assertions.*;

@DisplayName("Test fixture builders unit tests:")
class TestFixtureBuildersUnitTest {
    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void createValidator() {
        var configuration = Validation.byDefaultProvider().configure();
        var delegate = configuration.getDefaultConstraintValidatorFactory();
        validatorFactory = configuration.clockProvider(() -> TimeConstants.FIXED_CLOCK)
                .constraintValidatorFactory(new ConstraintValidatorFactory() {
                    @Override
                    public <T extends ConstraintValidator<?, ?>> T getInstance(Class<T> type) {
                        if (type == MinFutureDateOffsetValidator.class) {
                            return type.cast(new MinFutureDateOffsetValidator(TimeConstants.FIXED_CLOCK));
                        }
                        if (type == ValidEventCapacityValidator.class) {
                            return type.cast(new ValidEventCapacityValidator(new CommunityPropertiesTestBuilder().build()));
                        }
                        return delegate.getInstance(type);
                    }

                    @Override
                    public void releaseInstance(ConstraintValidator<?, ?> instance) {
                        delegate.releaseInstance(instance);
                    }
                }).buildValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        validatorFactory.close();
    }

    @ParameterizedTest(name = "{0}: valid default")
    @MethodSource("fixtureBuilders")
    void whenBuildingStandardFixtureShouldSatisfyBeanValidation(String name, Supplier<?> build) {
        assertThat(validator.validate(build.get())).as(name).isEmpty();
    }

    @ParameterizedTest(name = "{0}: fresh deterministic build")
    @MethodSource("fixtureBuilders")
    void whenReusingBuilderShouldProduceFreshDeterministicFixtures(String name, Supplier<?> build) {
        Object first = build.get();
        Object second = build.get();

        assertThat(second).as(name).isNotSameAs(first);
        assertThat(second).usingRecursiveComparison().isEqualTo(first);
    }

    static Stream<Arguments> fixtureBuilders() {
        return Stream.of(
                Arguments.of("City", (Supplier<?>) new CityTestBuilder()::build),
                Arguments.of("Event", (Supplier<?>) new EventTestBuilder()::build),
                Arguments.of("User", (Supplier<?>) new UserTestBuilder()::build),
                Arguments.of("Role", (Supplier<?>) new RoleTestBuilder()::build),
                Arguments.of("Tag", (Supplier<?>) new TagTestBuilder()::build),
                Arguments.of("File", (Supplier<?>) new FileTestBuilder()::build),
                Arguments.of("Thread", (Supplier<?>) new ThreadTestBuilder()::build),
                Arguments.of("ThreadReply", (Supplier<?>) new ThreadReplyTestBuilder()::build),
                Arguments.of("Conversation", (Supplier<?>) new ConversationTestBuilder()::build),
                Arguments.of("ConversationParticipant", (Supplier<?>) new ConversationParticipantTestBuilder()::build),
                Arguments.of("Message", (Supplier<?>) new MessageTestBuilder()::build),
                Arguments.of("Notification", (Supplier<?>) new NotificationTestBuilder()::build),
                Arguments.of("ActivationToken", (Supplier<?>) new ActivationTokenTestBuilder()::build),
                Arguments.of("RefreshToken", (Supplier<?>) new RefreshTokenTestBuilder()::build),
                Arguments.of("AuthenticationRequest", (Supplier<?>) new AuthenticationRequestTestBuilder()::build),
                Arguments.of("RegisterRequest", (Supplier<?>) new RegisterRequestTestBuilder()::build),
                Arguments.of("RefreshTokenRequest", (Supplier<?>) new RefreshTokenRequestTestBuilder()::build),
                Arguments.of("EmailBasedRequest", (Supplier<?>) new EmailBasedRequestTestBuilder()::build),
                Arguments.of("ChangeUserDetailsDto", (Supplier<?>) new ChangeUserDetailsDtoTestBuilder()::build),
                Arguments.of("ChangeUserEmailDto", (Supplier<?>) new ChangeUserEmailDtoTestBuilder()::build),
                Arguments.of("ChangeUserPasswordDto", (Supplier<?>) new ChangeUserPasswordDtoTestBuilder()::build),
                Arguments.of("EventCreateDto", (Supplier<?>) new EventCreateDtoTestBuilder()::build),
                Arguments.of("EventDto", (Supplier<?>) new EventDtoTestBuilder()::build),
                Arguments.of("FileUploadDto", (Supplier<?>) new FileUploadDtoTestBuilder()::build),
                Arguments.of("FileOverviewDto", (Supplier<?>) new FileOverviewDtoTestBuilder()::build),
                Arguments.of("MultipartFile", (Supplier<?>) new MultipartFileTestBuilder()::buildMultipartFile),
                Arguments.of("ThreadCreateDto", (Supplier<?>) new ThreadCreateDtoTestBuilder()::build),
                Arguments.of("ThreadDto", (Supplier<?>) new ThreadDtoTestBuilder()::build),
                Arguments.of("ThreadReplyCreateDto", (Supplier<?>) new ThreadReplyCreateDtoTestBuilder()::build),
                Arguments.of("ThreadReplyDto", (Supplier<?>) new ThreadReplyDtoTestBuilder()::build),
                Arguments.of("SendDirectMessageDto", (Supplier<?>) new SendDirectMessageDtoTestBuilder()::build),
                Arguments.of("SendConversationMessageDto", (Supplier<?>) new SendConversationMessageDtoTestBuilder()::build),
                Arguments.of("PasswordResetToken", (Supplier<?>) new PasswordResetTokenTestBuilder()::build),
                Arguments.of("EmailChangeToken", (Supplier<?>) new EmailChangeTokenTestBuilder()::build),
                Arguments.of("AuthEmailDelivery", (Supplier<?>) new AuthEmailDeliveryTestBuilder()::build),
                Arguments.of("NotificationPreference", (Supplier<?>) new NotificationPreferenceTestBuilder()::build),
                Arguments.of("NotificationDevice", (Supplier<?>) new NotificationDeviceTestBuilder()::build),
                Arguments.of("NotificationDelivery", (Supplier<?>) new NotificationDeliveryTestBuilder()::build),
                Arguments.of("ResetPasswordRequest", (Supplier<?>) new ResetPasswordRequestTestBuilder()::build),
                Arguments.of("JwtUserDetails", (Supplier<?>) new JwtUserDetailsTestBuilder()::build),
                Arguments.of("ResolvedCity", (Supplier<?>) new ResolvedCityTestBuilder()::build),
                Arguments.of("CitySearchResult", (Supplier<?>) new CitySearchResultTestBuilder()::build),
                Arguments.of("RateLimitDecision", (Supplier<?>) new RateLimitDecisionTestBuilder()::build),
                Arguments.of("MarkConversationReadDto", (Supplier<?>) new MarkConversationReadDtoTestBuilder()::build),
                Arguments.of("UpdateNotificationPreferenceDto", (Supplier<?>) new UpdateNotificationPreferenceDtoTestBuilder()::build),
                Arguments.of("NotificationPreferenceDto", (Supplier<?>) new NotificationPreferenceDtoTestBuilder()::build),
                Arguments.of("UpdateNotificationPreferencesDto", (Supplier<?>) new UpdateNotificationPreferencesDtoTestBuilder()::build),
                Arguments.of("NotificationDeviceDto", (Supplier<?>) new NotificationDeviceDtoTestBuilder()::build),
                Arguments.of("RegisterNotificationDeviceDto", (Supplier<?>) new RegisterNotificationDeviceDtoTestBuilder()::build),
                Arguments.of("NotificationTemplate", (Supplier<?>) new NotificationTemplateTestBuilder()::build),
                Arguments.of("DeleteCurrentUserDto", (Supplier<?>) new DeleteCurrentUserDtoTestBuilder()::build),
                Arguments.of("MessageDto", (Supplier<?>) new MessageDtoTestBuilder()::build),
                Arguments.of("LocalSeedReportAccount", (Supplier<?>) new LocalSeedReportAccountTestBuilder()::build),
                Arguments.of("LocalSeedReportResource", (Supplier<?>) new LocalSeedReportResourceTestBuilder()::build),
                Arguments.of("LocalSeedReport", (Supplier<?>) new LocalSeedReportTestBuilder()::build),
                Arguments.of("LocalAuthEmail", (Supplier<?>) new LocalAuthEmailTestBuilder()::build),
                Arguments.of("EncryptedConversationContent", (Supplier<?>) new EncryptedConversationContentTestBuilder()::build),
                Arguments.of("InitialDirectMessage", (Supplier<?>) new InitialDirectMessageTestBuilder()::build),
                Arguments.of("DirectConversationPair", (Supplier<?>) new DirectConversationPairTestBuilder()::build),
                Arguments.of("AuthEmailSendResult", (Supplier<?>) new AuthEmailSendResultTestBuilder()::build),
                Arguments.of("NotificationSendResult", (Supplier<?>) new NotificationSendResultTestBuilder()::build),
                Arguments.of("FcmSendResult", (Supplier<?>) new FcmSendResultTestBuilder()::build),
                Arguments.of("AuthEmailProperties", (Supplier<?>) new AuthEmailPropertiesTestBuilder()::build),
                Arguments.of("AuthRateLimitProperties", (Supplier<?>) new AuthRateLimitPropertiesTestBuilder()::build),
                Arguments.of("AuthProperties", (Supplier<?>) new AuthPropertiesTestBuilder()::build),
                Arguments.of("NotificationDeliveryProperties", (Supplier<?>) new NotificationDeliveryPropertiesTestBuilder()::build),
                Arguments.of("NotificationDevicesProperties", (Supplier<?>) new NotificationDevicesPropertiesTestBuilder()::build),
                Arguments.of("NotificationEmailProperties", (Supplier<?>) new NotificationEmailPropertiesTestBuilder()::build),
                Arguments.of("NotificationRetentionProperties", (Supplier<?>) new NotificationRetentionPropertiesTestBuilder()::build),
                Arguments.of("NotificationProperties", (Supplier<?>) new NotificationPropertiesTestBuilder()::build),
                Arguments.of("MailProperties", (Supplier<?>) new MailPropertiesTestBuilder()::build),
                Arguments.of("ApiCorsProperties", (Supplier<?>) new ApiCorsPropertiesTestBuilder()::build),
                Arguments.of("SeedProperties", (Supplier<?>) new SeedPropertiesTestBuilder()::build),
                Arguments.of("FrontendProperties", (Supplier<?>) new FrontendPropertiesTestBuilder()::build),
                Arguments.of("JwtProperties", (Supplier<?>) new JwtPropertiesTestBuilder()::build),
                Arguments.of("FirebaseProperties", (Supplier<?>) new FirebasePropertiesTestBuilder()::build),
                Arguments.of("EncryptionKey", (Supplier<?>) new EncryptionKeyTestBuilder()::build),
                Arguments.of("EncryptionProperties", (Supplier<?>) new EncryptionPropertiesTestBuilder()::build),
                Arguments.of("CommunityProperties", (Supplier<?>) new CommunityPropertiesTestBuilder()::build),
                Arguments.of("IssuedRefreshToken", (Supplier<?>) new IssuedRefreshTokenTestBuilder()::build),
                Arguments.of("RefreshTokenUse", (Supplier<?>) new RefreshTokenUseTestBuilder()::build),
                Arguments.of("FileOverviewProjection", (Supplier<?>) new FileOverviewProjectionTestBuilder()::build),
                Arguments.of("FileContentProjection", (Supplier<?>) new FileContentProjectionTestBuilder()::build)
        );
    }

    @ParameterizedTest(name = "{0}: valid standard preset")
    @MethodSource("standardPresets")
    void whenBuildingStandardPresetShouldSatisfyBeanValidation(String name, Supplier<?> build) {
        assertThat(validator.validate(build.get())).as(name).isEmpty();
    }

    static Stream<Arguments> standardPresets() {
        return Stream.of(
                // Standard presets only; intentionally malformed fixtures are tested at their boundary.
                Arguments.of("ActivationTokenTestBuilder.firstToken", (Supplier<?>) ActivationTokenTestBuilder.firstToken()::build),
                Arguments.of("ActivationTokenTestBuilder.secondToken", (Supplier<?>) ActivationTokenTestBuilder.secondToken()::build),
                Arguments.of("AuthenticationRequestTestBuilder.authenticationRequestForFirstUser", (Supplier<?>) AuthenticationRequestTestBuilder.authenticationRequestForFirstUser()::build),
                Arguments.of("AuthenticationRequestTestBuilder.authenticationRequestForSecondUser", (Supplier<?>) AuthenticationRequestTestBuilder.authenticationRequestForSecondUser()::build),
                Arguments.of("CityTestBuilder.warsaw", (Supplier<?>) CityTestBuilder.warsaw()::build),
                Arguments.of("CityTestBuilder.krakow", (Supplier<?>) CityTestBuilder.krakow()::build),
                Arguments.of("CityTestBuilder.systemCity", (Supplier<?>) CityTestBuilder.systemCity()::build),
                Arguments.of("ConversationParticipantTestBuilder.firstConversationParticipant", (Supplier<?>) ConversationParticipantTestBuilder.firstConversationParticipant()::build),
                Arguments.of("ConversationParticipantTestBuilder.secondConversationParticipant", (Supplier<?>) ConversationParticipantTestBuilder.secondConversationParticipant()::build),
                Arguments.of("ConversationTestBuilder.firstDirectConversation", (Supplier<?>) ConversationTestBuilder.firstDirectConversation()::build),
                Arguments.of("ConversationTestBuilder.secondDirectConversation", (Supplier<?>) ConversationTestBuilder.secondDirectConversation()::build),
                Arguments.of("EventTestBuilder.firstEvent", (Supplier<?>) EventTestBuilder.firstEvent()::build),
                Arguments.of("EventTestBuilder.secondEvent", (Supplier<?>) EventTestBuilder.secondEvent()::build),
                Arguments.of("EventTestBuilder.pastEvent", (Supplier<?>) EventTestBuilder.pastEvent()::build),
                Arguments.of("FileTestBuilder.jpgFile", (Supplier<?>) FileTestBuilder.jpgFile()::build),
                Arguments.of("FileTestBuilder.jpegFile", (Supplier<?>) FileTestBuilder.jpegFile()::build),
                Arguments.of("FileTestBuilder.pngFile", (Supplier<?>) FileTestBuilder.pngFile()::build),
                Arguments.of("FileTestBuilder.pdfFile", (Supplier<?>) FileTestBuilder.pdfFile()::build),
                Arguments.of("FileTestBuilder.docFile", (Supplier<?>) FileTestBuilder.docFile()::build),
                Arguments.of("FileTestBuilder.docxFile", (Supplier<?>) FileTestBuilder.docxFile()::build),
                Arguments.of("FileTestBuilder.odtFile", (Supplier<?>) FileTestBuilder.odtFile()::build),
                Arguments.of("FileTestBuilder.pptFile", (Supplier<?>) FileTestBuilder.pptFile()::build),
                Arguments.of("FileTestBuilder.pptxFile", (Supplier<?>) FileTestBuilder.pptxFile()::build),
                Arguments.of("FileTestBuilder.xlsFile", (Supplier<?>) FileTestBuilder.xlsFile()::build),
                Arguments.of("FileTestBuilder.xlsxFile", (Supplier<?>) FileTestBuilder.xlsxFile()::build),
                Arguments.of("FileTestBuilder.mp4File", (Supplier<?>) FileTestBuilder.mp4File()::build),
                Arguments.of("FileTestBuilder.aviFile", (Supplier<?>) FileTestBuilder.aviFile()::build),
                Arguments.of("MessageTestBuilder.firstMessage", (Supplier<?>) MessageTestBuilder.firstMessage()::build),
                Arguments.of("MessageTestBuilder.inverseMessage", (Supplier<?>) MessageTestBuilder.inverseMessage()::build),
                Arguments.of("MessageTestBuilder.thirdMessage", (Supplier<?>) MessageTestBuilder.thirdMessage()::build),
                Arguments.of("NotificationTestBuilder.privateMessageNotification", (Supplier<?>) NotificationTestBuilder.privateMessageNotification()::build),
                Arguments.of("NotificationTestBuilder.threadReplyNotification", (Supplier<?>) NotificationTestBuilder.threadReplyNotification()::build),
                Arguments.of("NotificationTestBuilder.eventUpdateNotification", (Supplier<?>) NotificationTestBuilder.eventUpdateNotification()::build),
                Arguments.of("NotificationTestBuilder.newEventFileNotification", (Supplier<?>) NotificationTestBuilder.newEventFileNotification()::build),
                Arguments.of("NotificationTestBuilder.newEventThreadNotification", (Supplier<?>) NotificationTestBuilder.newEventThreadNotification()::build),
                Arguments.of("RefreshTokenTestBuilder.firstRefreshToken", (Supplier<?>) RefreshTokenTestBuilder.firstRefreshToken()::build),
                Arguments.of("RefreshTokenTestBuilder.secondRefreshToken", (Supplier<?>) RefreshTokenTestBuilder.secondRefreshToken()::build),
                Arguments.of("RefreshTokenTestBuilder.expiredRefreshToken", (Supplier<?>) RefreshTokenTestBuilder.expiredRefreshToken()::build),
                Arguments.of("RefreshTokenTestBuilder.revokedRefreshToken", (Supplier<?>) RefreshTokenTestBuilder.revokedRefreshToken()::build),
                Arguments.of("RoleTestBuilder.userRole", (Supplier<?>) RoleTestBuilder.userRole()::build),
                Arguments.of("RoleTestBuilder.adminRole", (Supplier<?>) RoleTestBuilder.adminRole()::build),
                Arguments.of("TagTestBuilder.firstTag", (Supplier<?>) TagTestBuilder.firstTag()::build),
                Arguments.of("TagTestBuilder.secondTag", (Supplier<?>) TagTestBuilder.secondTag()::build),
                Arguments.of("TagTestBuilder.thirdTag", (Supplier<?>) TagTestBuilder.thirdTag()::build),
                Arguments.of("ThreadReplyTestBuilder.firstReply", (Supplier<?>) ThreadReplyTestBuilder.firstReply()::build),
                Arguments.of("ThreadReplyTestBuilder.secondReply", (Supplier<?>) ThreadReplyTestBuilder.secondReply()::build),
                Arguments.of("ThreadReplyTestBuilder.thirdReply", (Supplier<?>) ThreadReplyTestBuilder.thirdReply()::build),
                Arguments.of("ThreadReplyTestBuilder.oldReply", (Supplier<?>) ThreadReplyTestBuilder.oldReply()::build),
                Arguments.of("ThreadTestBuilder.firstThread", (Supplier<?>) ThreadTestBuilder.firstThread()::build),
                Arguments.of("ThreadTestBuilder.secondThread", (Supplier<?>) ThreadTestBuilder.secondThread()::build),
                Arguments.of("ThreadTestBuilder.oldThread", (Supplier<?>) ThreadTestBuilder.oldThread()::build),
                Arguments.of("ThreadTestBuilder.randomThread", (Supplier<?>) ThreadTestBuilder.randomThread()::build),
                Arguments.of("UserTestBuilder.firstUser", (Supplier<?>) UserTestBuilder.firstUser()::build),
                Arguments.of("UserTestBuilder.secondUser", (Supplier<?>) UserTestBuilder.secondUser()::build),
                Arguments.of("UserTestBuilder.thirdUser", (Supplier<?>) UserTestBuilder.thirdUser()::build),
                Arguments.of("UserTestBuilder.adminUser", (Supplier<?>) UserTestBuilder.adminUser()::build),
                Arguments.of("UserTestBuilder.deletedUser", (Supplier<?>) UserTestBuilder.deletedUser()::build),
                Arguments.of("ChangeUserDetailsDtoTestBuilder.validUpdate", (Supplier<?>) ChangeUserDetailsDtoTestBuilder.validUpdate()::build),
                Arguments.of("ChangeUserEmailDtoTestBuilder.validChange", (Supplier<?>) ChangeUserEmailDtoTestBuilder.validChange()::build),
                Arguments.of("ChangeUserPasswordDtoTestBuilder.validChange", (Supplier<?>) ChangeUserPasswordDtoTestBuilder.validChange()::build),
                Arguments.of("EmailBasedRequestTestBuilder.firstUser", (Supplier<?>) EmailBasedRequestTestBuilder.firstUser()::build),
                Arguments.of("EmailBasedRequestTestBuilder.thirdUser", (Supplier<?>) EmailBasedRequestTestBuilder.thirdUser()::build),
                Arguments.of("EmailBasedRequestTestBuilder.nonExistingUser", (Supplier<?>) EmailBasedRequestTestBuilder.nonExistingUser()::build),
                Arguments.of("EventCreateDtoTestBuilder.firstEvent", (Supplier<?>) EventCreateDtoTestBuilder.firstEvent()::build),
                Arguments.of("EventCreateDtoTestBuilder.secondEvent", (Supplier<?>) EventCreateDtoTestBuilder.secondEvent()::build),
                Arguments.of("EventCreateDtoTestBuilder.updatedEvent", (Supplier<?>) EventCreateDtoTestBuilder.updatedEvent()::build),
                Arguments.of("FileUploadDtoTestBuilder.jpgFile", (Supplier<?>) FileUploadDtoTestBuilder.jpgFile()::build),
                Arguments.of("MultipartFileTestBuilder.jpgFile", (Supplier<?>) MultipartFileTestBuilder.jpgFile()::buildMultipartFile),
                Arguments.of("MultipartFileTestBuilder.pngFile", (Supplier<?>) MultipartFileTestBuilder.pngFile()::buildMultipartFile),
                Arguments.of("MultipartFileTestBuilder.pdfFile", (Supplier<?>) MultipartFileTestBuilder.pdfFile()::buildMultipartFile),
                Arguments.of("MultipartFileTestBuilder.docxFile", (Supplier<?>) MultipartFileTestBuilder.docxFile()::buildMultipartFile),
                Arguments.of("RefreshTokenRequestTestBuilder.firstToken", (Supplier<?>) RefreshTokenRequestTestBuilder.firstToken()::build),
                Arguments.of("RefreshTokenRequestTestBuilder.secondToken", (Supplier<?>) RefreshTokenRequestTestBuilder.secondToken()::build),
                Arguments.of("RegisterRequestTestBuilder.firstUserRegisterRequest", (Supplier<?>) RegisterRequestTestBuilder.firstUserRegisterRequest()::build),
                Arguments.of("RegisterRequestTestBuilder.secondUserRegisterRequest", (Supplier<?>) RegisterRequestTestBuilder.secondUserRegisterRequest()::build),
                Arguments.of("RegisterRequestTestBuilder.thirdUserRegisterRequest", (Supplier<?>) RegisterRequestTestBuilder.thirdUserRegisterRequest()::build),
                Arguments.of("SendConversationMessageDtoTestBuilder.firstConversationMessage", (Supplier<?>) SendConversationMessageDtoTestBuilder.firstConversationMessage()::build),
                Arguments.of("SendConversationMessageDtoTestBuilder.secondConversationMessage", (Supplier<?>) SendConversationMessageDtoTestBuilder.secondConversationMessage()::build),
                Arguments.of("SendDirectMessageDtoTestBuilder.firstDirectMessage", (Supplier<?>) SendDirectMessageDtoTestBuilder.firstDirectMessage()::build),
                Arguments.of("SendDirectMessageDtoTestBuilder.inverseDirectMessage", (Supplier<?>) SendDirectMessageDtoTestBuilder.inverseDirectMessage()::build),
                Arguments.of("ThreadCreateDtoTestBuilder.firstThread", (Supplier<?>) ThreadCreateDtoTestBuilder.firstThread()::build),
                Arguments.of("ThreadCreateDtoTestBuilder.firstThreadUpdate", (Supplier<?>) ThreadCreateDtoTestBuilder.firstThreadUpdate()::build),
                Arguments.of("ThreadReplyCreateDtoTestBuilder.firstReply", (Supplier<?>) ThreadReplyCreateDtoTestBuilder.firstReply()::build),
                Arguments.of("ThreadReplyCreateDtoTestBuilder.firstReplyUpdate", (Supplier<?>) ThreadReplyCreateDtoTestBuilder.firstReplyUpdate()::build)
        );
    }

    @Nested
    @DisplayName("Conversation fixture graph tests:")
    class ConversationFixtureGraphTests {
        @Test
        void whenBuildingStandaloneParticipantShouldCreateOneLinkedParticipantWithoutRecursion() {
            var builder = ConversationParticipantTestBuilder.firstConversationParticipant();
            var first = builder.build();
            var second = builder.build();

            assertThat(first.getConversation().getParticipants()).containsExactly(first);
            assertThat(second.getConversation()).isNotSameAs(first.getConversation());
            assertThat(second.getConversation().getParticipants()).containsExactly(second);
            assertThat(validator.validate(first.getConversation())).isEmpty();
            assertThat(validator.validate(first)).isEmpty();
            assertThat(validator.validate(ConversationParticipantTestBuilder.secondConversationParticipant().build())).isEmpty();
        }

        @Test
        void whenBuildingDirectConversationShouldHaveExactlyTwoParticipantsWithCorrectParent() {
            var first = ConversationTestBuilder.firstDirectConversation().build();
            var second = ConversationTestBuilder.secondDirectConversation().build();

            for (var conversation : List.of(first, second)) {
                assertThat(conversation.getParticipants()).hasSize(2);
                for (var participant : conversation.getParticipants()) {
                    assertThat(participant.getConversation()).isSameAs(conversation);
                    assertThat(validator.validate(participant)).isEmpty();
                }
            }
            assertThat(first.getParticipants()).extracting(p -> p.getUser().getId())
                    .containsExactlyInAnyOrder(UserConstants.FIRST_USER_ID, UserConstants.SECOND_USER_ID);
            assertThat(second.getParticipants()).extracting(p -> p.getUser().getId())
                    .containsExactlyInAnyOrder(UserConstants.FIRST_USER_ID, UserConstants.THIRD_USER_ID);
        }

        @Test
        void whenProvidingParticipantParentShouldKeepReferenceWithoutAttachingImplicitly() {
            var parent = new ConversationTestBuilder().buildWithoutParticipants();
            var participant = new ConversationParticipantTestBuilder().conversation(parent).build();

            assertThat(participant.getConversation()).isSameAs(parent);
            assertThat(parent.getParticipants()).isEmpty();
        }

        @Test
        void whenExplicitlyRemovingParticipantParentShouldExposeValidationFailure() {
            var participant = new ConversationParticipantTestBuilder().conversation(null).build();

            assertThat(participant.getConversation()).isNull();
            assertThat(validator.validate(participant)).extracting(v -> v.getPropertyPath().toString())
                    .contains("conversation");
        }
    }

    @Nested
    @DisplayName("Fixture field API tests:")
    class FixtureFieldApiTests {
        @Test
        void whenSettingCityCoordinatesShouldUseExactValuesAndKeepConstructorInvariants() {
            var city = CityTestBuilder.warsaw().latitude(-90).longitude(180).build();

            assertThat(city.getLatitude()).isEqualTo(-90);
            assertThat(city.getLongitude()).isEqualTo(180);
            assertThatThrownBy(() -> CityTestBuilder.warsaw().latitude(91).build())
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void whenOverridingEventFieldsShouldPassNullAndInvalidValuesWithoutRepair() {
            var event = EventTestBuilder.firstEvent().exactAddress(" ").lastUpdate(null).maxAttendees(null)
                    .attendeeCount(-1).build();

            assertThat(event.getExactAddress()).isEqualTo(" ");
            assertThat(event.getLastUpdate()).isNull();
            assertThat(event.getMaxAttendees()).isNull();
            assertThat(event.getAttendeeCount()).isEqualTo(-1);
            assertThat(validator.validate(event)).extracting(v -> v.getPropertyPath().toString())
                    .contains("exactAddress", "lastUpdate", "attendeeCount");
        }

        @Test
        void whenOverridingThreadActivityShouldNotChangeOtherTimestamps() {
            var thread = ThreadTestBuilder.firstThread().lastActivity(TimeConstants.ONE_WEEK_AGO).build();

            assertThat(thread.getLastActivity()).isEqualTo(TimeConstants.ONE_WEEK_AGO);
            assertThat(thread.getCreateDate()).isEqualTo(TimeConstants.ONE_HOUR_AGO);
            assertThat(thread.getLastUpdate()).isEqualTo(TimeConstants.NOW);
            assertThat(ThreadTestBuilder.firstThread().lastActivity(null).build().getLastActivity()).isNull();
        }

        @Test
        void whenOverridingUserPreferenceVersionShouldNotChangeSecurityVersion() {
            var user = UserTestBuilder.firstUser().notificationPreferencesVersion(-1).securityVersion(7).build();

            assertThat(user.getNotificationPreferencesVersion()).isEqualTo(-1);
            assertThat(user.getSecurityVersion()).isEqualTo(7);
            assertThat(validator.validate(user)).extracting(v -> v.getPropertyPath().toString())
                    .containsExactly("notificationPreferencesVersion");
        }
    }

    @Nested
    @DisplayName("Token fixture tests:")
    class TokenFixtureTests {
        @Test
        void whenBuildingRefreshTokenPresetsShouldUseStableDistinctFamiliesAndHonorNull() {
            assertThat(RefreshTokenTestBuilder.firstRefreshToken().build().getFamilyId())
                    .isEqualTo(RefreshTokenConstants.FIRST_REFRESH_TOKEN_FAMILY_ID);
            assertThat(RefreshTokenTestBuilder.secondRefreshToken().build().getFamilyId())
                    .isEqualTo(RefreshTokenConstants.SECOND_REFRESH_TOKEN_FAMILY_ID)
                    .isNotEqualTo(RefreshTokenConstants.FIRST_REFRESH_TOKEN_FAMILY_ID);
            assertThat(RefreshTokenTestBuilder.expiredRefreshToken().build().getFamilyId())
                    .isEqualTo(RefreshTokenConstants.THIRD_REFRESH_TOKEN_FAMILY_ID);
            assertThat(RefreshTokenTestBuilder.firstRefreshToken().familyId(null).build().getFamilyId()).isNull();
        }

        @Test
        void whenBuildingIssuedTokenShouldReuseConsistentHashAndRawCredential() {
            var issued = new IssuedRefreshTokenTestBuilder().build();
            var use = new RefreshTokenUseTestBuilder().build();

            assertThat(issued.rawToken()).isEqualTo(RefreshTokenConstants.FIRST_REFRESH_TOKEN);
            assertThat(issued.refreshToken().getTokenHash()).isEqualTo(RefreshTokenTestBuilder.hashOf(issued.rawToken()));
            assertThat(use.rawToken()).isEqualTo(issued.rawToken());
            assertThat(use.refreshToken().getTokenHash()).isEqualTo(issued.refreshToken().getTokenHash());
        }

        @Test
        void whenOverridingTokenParentShouldPreserveReferenceAndNotRepairItsHash() {
            var token = RefreshTokenTestBuilder.firstRefreshToken().build();
            String hash = token.getTokenHash();

            var issued = new IssuedRefreshTokenTestBuilder().refreshToken(token).rawToken("different-raw-token").build();
            var use = new RefreshTokenUseTestBuilder().refreshToken(token).rawToken("different-raw-token").build();

            assertThat(issued.refreshToken()).isSameAs(token);
            assertThat(use.refreshToken()).isSameAs(token);
            assertThat(token.getTokenHash()).isEqualTo(hash);
            assertThat(issued.rawToken()).isEqualTo("different-raw-token");
        }

        @Test
        void whenPassingNullToTokenWrappersShouldNotSupplyFallback() {
            assertThat(new IssuedRefreshTokenTestBuilder().refreshToken(null).build().refreshToken()).isNull();
            assertThat(new RefreshTokenUseTestBuilder().refreshToken(null).build().refreshToken()).isNull();
            var issued = new IssuedRefreshTokenTestBuilder().rawToken(null).build();
            assertThat(issued.rawToken()).isNull();
            assertThat(issued.refreshToken().getTokenHash()).isNull();
            assertThat(new RefreshTokenUseTestBuilder().rawToken(null).build().rawToken()).isNull();
        }

        @Test
        void whenPreparingUnissuedActivationTokenShouldLeaveIssuanceToAct() {
            var token = ActivationTokenTestBuilder.firstToken().unissued().id(null).build();
            assertThat(token.getTokenHash()).isNull();
            assertThat(token.getExpirationDate()).isNull();
            assertThat(token.getId()).isNull();

            token.issue(ActivationTokenConstants.FIRST_ACTIVATION_TOKEN_UUID, 60_000, TimeConstants.NOW);

            assertThat(token.matches(ActivationTokenConstants.FIRST_ACTIVATION_TOKEN_UUID)).isTrue();
            assertThat(token.getExpirationDate()).isEqualTo(TokenFixtureConstants.EXPIRATION_DATE);
        }

        @Test
        void whenPreparingUnissuedResetTokenShouldLeaveIssuanceToAct() {
            var token = new PasswordResetTokenTestBuilder().unissued().build();
            assertThat(token.getTokenHash()).isNull();
            assertThat(token.getExpirationDate()).isNull();

            token.issue(ActivationTokenConstants.FIRST_ACTIVATION_TOKEN_UUID, 60_000, TimeConstants.NOW);

            assertThat(token.getTokenHash()).isEqualTo(RefreshTokenTestBuilder.hashOf(
                    ActivationTokenConstants.FIRST_ACTIVATION_TOKEN_UUID.toString()));
            assertThat(token.getExpirationDate()).isEqualTo(TokenFixtureConstants.EXPIRATION_DATE);
        }

        @Test
        void whenPreparingUnissuedEmailChangeTokenShouldLeaveIssuanceToAct() {
            var token = new EmailChangeTokenTestBuilder().unissued().build();
            assertThat(token.getTokenHash()).isNull();
            assertThat(token.getExpirationDate()).isNull();
            assertThat(token.getPendingEmail()).isNull();

            token.issue(ActivationTokenConstants.FIRST_ACTIVATION_TOKEN_UUID, UserConstants.FIRST_USER_NEW_EMAIL,
                    60_000, TimeConstants.NOW);

            assertThat(token.getTokenHash()).isEqualTo(RefreshTokenTestBuilder.hashOf(
                    ActivationTokenConstants.FIRST_ACTIVATION_TOKEN_UUID.toString()));
            assertThat(token.getPendingEmail()).isEqualTo(UserConstants.FIRST_USER_NEW_EMAIL);
        }

        @Test
        void whenBuildingActivationTokenShouldConstructHashWithoutIssuanceWorkflow() {
            var token = ActivationTokenTestBuilder.firstToken().build();
            assertThat(token.matches(ActivationTokenConstants.FIRST_ACTIVATION_TOKEN_UUID)).isTrue();
            assertThat(token.getExpirationDate()).isEqualTo(
                    TimeConstants.NOW.plusMillis(ActivationTokenConstants.ACTIVATION_TOKEN_EXPIRATION_MILLIS));
            assertThat(ActivationTokenTestBuilder.firstToken().rawToken(null).expirationDate(null).build().getTokenHash()).isNull();
        }
    }

    @ParameterizedTest(name = "{0}: generated ID left null")
    @MethodSource("newPersistentEntities")
    void whenPassingNullGeneratedIdShouldLeaveAssignmentToPersistence(String name, Supplier<?> build) {
        assertThat(build.get()).as(name).extracting("id").isNull();
    }

    static Stream<Arguments> newPersistentEntities() {
        return Stream.of(
                Arguments.of("PasswordResetToken", (Supplier<?>) new PasswordResetTokenTestBuilder().id(null)::build),
                Arguments.of("EmailChangeToken", (Supplier<?>) new EmailChangeTokenTestBuilder().id(null)::build),
                Arguments.of("AuthEmailDelivery", (Supplier<?>) new AuthEmailDeliveryTestBuilder().id(null)::build),
                Arguments.of("NotificationPreference", (Supplier<?>) new NotificationPreferenceTestBuilder().id(null)::build),
                Arguments.of("NotificationDevice", (Supplier<?>) new NotificationDeviceTestBuilder().id(null)::build),
                Arguments.of("NotificationDelivery", (Supplier<?>) new NotificationDeliveryTestBuilder().id(null)::build),
                Arguments.of("ActivationToken", (Supplier<?>) ActivationTokenTestBuilder.firstToken().id(null)::build),
                Arguments.of("RefreshToken", (Supplier<?>) RefreshTokenTestBuilder.firstRefreshToken().id(null)::build)
        );
    }

    @Nested
    @DisplayName("Explicit override tests:")
    class ExplicitOverrideTests {
        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" ", "\t", "Provider:ID", "  Provider:ID ", "test:WaRsAw", "Warsaw"})
        void whenOverridingDtoCityIdShouldPassExactInputWithoutNormalization(String externalId) {
            assertThat(new RegisterRequestTestBuilder().homeCityExternalId(externalId).build().getHomeCityExternalId())
                    .isEqualTo(externalId);
            assertThat(new ChangeUserDetailsDtoTestBuilder().homeCityExternalId(externalId).build().getHomeCityExternalId())
                    .isEqualTo(externalId);
            assertThat(new EventCreateDtoTestBuilder().cityExternalId(externalId).build().getCityExternalId())
                    .isEqualTo(externalId);
        }

        @Test
        void whenUsingCityPresetsShouldUseReadyExternalIdsIndependentOfDisplayName() {
            assertThat(CityTestBuilder.warsaw().build().getExternalId()).isEqualTo(CitiesConstants.WARSAW_EXTERNAL_ID);
            assertThat(CityTestBuilder.krakow().build().getExternalId()).isEqualTo(CitiesConstants.KRAKOW_EXTERNAL_ID);
            assertThat(CityTestBuilder.systemCity().build().getExternalId()).isEqualTo(CitiesConstants.SYSTEM_CITY_EXTERNAL_ID);
            assertThat(CityTestBuilder.warsaw().name("Custom label").build().getExternalId())
                    .isEqualTo(CitiesConstants.WARSAW_EXTERNAL_ID);
            assertThat(new RegisterRequestTestBuilder().build().getHomeCityExternalId()).isEqualTo(CitiesConstants.WARSAW_EXTERNAL_ID);
            assertThat(new ChangeUserDetailsDtoTestBuilder().build().getHomeCityExternalId()).isEqualTo(CitiesConstants.KRAKOW_EXTERNAL_ID);
            assertThat(EventCreateDtoTestBuilder.updatedEvent().build().getCityExternalId()).isEqualTo(CitiesConstants.KRAKOW_EXTERNAL_ID);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" ", "\t"})
        void whenPassingInvalidCityIdShouldReachProductionConstructorInsteadOfFallback(String externalId) {
            assertThatThrownBy(() -> CityTestBuilder.warsaw().externalId(externalId).build())
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("externalId");
        }

        @Test
        void whenBuildingCityShouldKeepProductionTrimmingWithoutExtraPrefixOrCaseChanges() {
            var city = CityTestBuilder.warsaw().externalId("  Provider:ID ").countryCode(" pl ").build();
            assertThat(city.getExternalId()).isEqualTo("Provider:ID");
            assertThat(city.getCountryCode()).isEqualTo("PL");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" ", "  Historical Name  "})
        void whenOverridingSnapshotShouldNotDeriveItFromUserInEitherFluentOrder(String snapshot) {
            var user = UserTestBuilder.thirdUser().build();
            var message = MessageTestBuilder.inverseMessage().senderNameAtCreation(snapshot).sender(user).build();
            var reversedMessage = MessageTestBuilder.inverseMessage().sender(user).senderNameAtCreation(snapshot).build();
            var participant = ConversationParticipantTestBuilder.secondConversationParticipant()
                    .userNameAtJoin(snapshot).user(user).build();
            var reversedParticipant = ConversationParticipantTestBuilder.secondConversationParticipant()
                    .user(user).userNameAtJoin(snapshot).build();

            assertThat(message.getSender()).isSameAs(user);
            assertThat(message.getSenderNameAtCreation()).isEqualTo(snapshot);
            assertThat(reversedMessage.getSenderNameAtCreation()).isEqualTo(snapshot);
            assertThat(participant.getUser()).isSameAs(user);
            assertThat(participant.getUserNameAtJoin()).isEqualTo(snapshot);
            assertThat(reversedParticipant.getUserNameAtJoin()).isEqualTo(snapshot);
            if (snapshot == null || snapshot.isBlank()) {
                assertThat(validator.validate(message)).extracting(v -> v.getPropertyPath().toString())
                        .contains("senderNameAtCreation");
                assertThat(validator.validate(participant)).extracting(v -> v.getPropertyPath().toString())
                        .contains("userNameAtJoin");
            }
        }

        @Test
        void whenUsingSnapshotPresetsShouldHaveExplicitNamesIndependentOfRelationshipOverrides() {
            assertThat(MessageTestBuilder.firstMessage().build().getSenderNameAtCreation()).isEqualTo(UserConstants.FIRST_USER_FULL_NAME);
            assertThat(MessageTestBuilder.inverseMessage().build().getSenderNameAtCreation()).isEqualTo(UserConstants.SECOND_USER_FULL_NAME);
            assertThat(ConversationParticipantTestBuilder.firstConversationParticipant().build().getUserNameAtJoin())
                    .isEqualTo(UserConstants.FIRST_USER_FULL_NAME);
            assertThat(ConversationParticipantTestBuilder.secondConversationParticipant().build().getUserNameAtJoin())
                    .isEqualTo(UserConstants.SECOND_USER_FULL_NAME);
            assertThat(MessageTestBuilder.inverseMessage().sender(null).build().getSenderNameAtCreation())
                    .isEqualTo(UserConstants.SECOND_USER_FULL_NAME);
            assertThat(ConversationParticipantTestBuilder.secondConversationParticipant().user(null).build().getUserNameAtJoin())
                    .isEqualTo(UserConstants.SECOND_USER_FULL_NAME);
            assertThat(ConversationTestBuilder.secondDirectConversation().build().getParticipants())
                    .extracting(p -> p.getUserNameAtJoin())
                    .containsExactlyInAnyOrder(UserConstants.FIRST_USER_FULL_NAME, UserConstants.THIRD_USER_FULL_NAME);
        }

        @ParameterizedTest
        @MethodSource("replyTimestamps")
        void whenOverridingReplyTimestampsShouldKeepFieldsIndependentInEitherOrder(Instant replyDate, Instant lastUpdate) {
            var first = ThreadReplyTestBuilder.firstReply().lastUpdate(lastUpdate).replyDate(replyDate).build();
            var second = ThreadReplyTestBuilder.firstReply().replyDate(replyDate).lastUpdate(lastUpdate).build();
            assertThat(first.getReplyDate()).isEqualTo(replyDate);
            assertThat(first.getLastUpdate()).isEqualTo(lastUpdate);
            assertThat(second.getReplyDate()).isEqualTo(replyDate);
            assertThat(second.getLastUpdate()).isEqualTo(lastUpdate);
        }

        static Stream<Arguments> replyTimestamps() {
            return Stream.of(
                    Arguments.of(TimeConstants.TWO_HOURS_AGO, TimeConstants.NOW),
                    Arguments.of(null, TimeConstants.NOW),
                    Arguments.of(TimeConstants.TWO_HOURS_AGO, null),
                    Arguments.of(null, null));
        }

        @Test
        void whenOverridingOnlyReplyDateShouldLeavePresetLastUpdateUntouched() {
            var reply = ThreadReplyTestBuilder.firstReply().replyDate(TimeConstants.TWO_HOURS_AGO).build();
            assertThat(reply.getLastUpdate()).isEqualTo(TimeConstants.ONE_HOUR_AGO);
            var oldReply = ThreadReplyTestBuilder.oldReply().build();
            assertThat(oldReply.getReplyDate()).isEqualTo(TimeConstants.SEVEN_HOURS_AGO);
            assertThat(oldReply.getLastUpdate()).isEqualTo(TimeConstants.SEVEN_HOURS_AGO);
        }

        @Test
        void whenProvidingReplyParentShouldKeepReferenceWithoutChangingExistingCollection() {
            var thread = ThreadTestBuilder.firstThread().build();
            var existing = ThreadReplyTestBuilder.firstReply().thread(thread).build();
            thread.addReplyToThread(existing);
            var reply = ThreadReplyTestBuilder.secondReply().thread(thread).build();
            assertThat(reply.getThread()).isSameAs(thread);
            assertThat(thread.getReplies()).containsExactly(existing);
        }

        @Test
        void whenExplicitlyRemovingReplyAndMessageParentsShouldReachValidationBoundary() {
            var file = FileTestBuilder.jpgFile().event(null).content(null).build();
            assertThat(file.getEvent()).isNull();
            assertThat(file.getContent()).isNull();
            assertThat(validator.validate(file)).extracting(v -> v.getPropertyPath().toString())
                    .contains("event", "content");
            var reply = ThreadReplyTestBuilder.firstReply().thread(null).content(null).build();
            var reversed = ThreadReplyTestBuilder.firstReply().content(null).thread(null).build();
            for (var fixture : List.of(reply, reversed)) {
                assertThat(fixture.getThread()).isNull();
                assertThat(fixture.getContent()).isNull();
                assertThat(validator.validate(fixture)).extracting(v -> v.getPropertyPath().toString()).contains("thread", "content");
            }
            var message = MessageTestBuilder.firstMessage().conversation(null).senderNameAtCreation(null).build();
            assertThat(message.getConversation()).isNull();
            assertThat(validator.validate(message)).extracting(v -> v.getPropertyPath().toString())
                    .contains("conversation", "senderNameAtCreation");
        }

        @Test
        void whenPassingNullEntityRelationshipsShouldPreserveMinimalGraph() {
            assertThat(new PasswordResetTokenTestBuilder().user(null).build().getUser()).isNull();
            assertThat(new EmailChangeTokenTestBuilder().user(null).build().getUser()).isNull();
            assertThat(new NotificationDeliveryTestBuilder().notification(null).build().getNotification()).isNull();
            assertThat(new InitialDirectMessageTestBuilder().message(null).build().message()).isNull();
        }

        @Test
        void whenOverridingEntityRelationshipShouldPreserveReferenceWithoutParentMutation() {
            var user = UserTestBuilder.firstUser().build();
            var notification = NotificationTestBuilder.eventUpdateNotification().build();
            var deliveries = new java.util.HashSet<>(notification.getDeliveries());

            assertThat(new PasswordResetTokenTestBuilder().user(user).build().getUser()).isSameAs(user);
            assertThat(new EmailChangeTokenTestBuilder().user(user).build().getUser()).isSameAs(user);
            assertThat(new NotificationDeliveryTestBuilder().notification(notification).build().getNotification()).isSameAs(notification);
            assertThat(notification.getDeliveries()).isEqualTo(deliveries);
        }

        @Test
        void whenOverridingCityDataShouldPreserveNullWhitespaceAndCoordinates() {
            var resolved = new ResolvedCityTestBuilder().externalId("  Provider:ID ").name(null)
                    .countryCode("pl ").timeZoneId(null).latitude(-91).longitude(181).build();
            var search = new CitySearchResultTestBuilder().externalId("  Provider:ID ").displayName(" ").build();

            assertThat(resolved.externalId()).isEqualTo("  Provider:ID ");
            assertThat(resolved.name()).isNull();
            assertThat(resolved.countryCode()).isEqualTo("pl ");
            assertThat(resolved.timeZoneId()).isNull();
            assertThat(resolved.latitude()).isEqualTo(-91);
            assertThat(resolved.longitude()).isEqualTo(181);
            assertThat(search.externalId()).isEqualTo(resolved.externalId());
            assertThat(search.displayName()).isEqualTo(" ");
        }

        @Test
        void whenBuildingInvalidRequestsShouldLeaveValidationToBoundary() {
            var password = new ResetPasswordRequestTestBuilder().password(" ").passwordConfirmation(null).build();
            var deletion = new DeleteCurrentUserDtoTestBuilder().password(null).build();
            var preferences = new UpdateNotificationPreferencesDtoTestBuilder().version(-1L).preferences(null).build();
            var device = new RegisterNotificationDeviceDtoTestBuilder().platform(null).firebaseInstallationId(" ").build();

            assertThat(password.password()).isEqualTo(" ");
            assertThat(password.passwordConfirmation()).isNull();
            assertThat(deletion.password()).isNull();
            assertThat(preferences.preferences()).isNull();
            assertThat(preferences.version()).isEqualTo(-1L);
            assertThat(device.platform()).isNull();
            assertThat(device.firebaseInstallationId()).isEqualTo(" ");
            assertThat(validator.validate(password)).isNotEmpty();
            assertThat(validator.validate(device)).isNotEmpty();
        }

        @Test
        void whenOverridingSendResultsShouldNotClassifyOrRepairCounts() {
            var fcm = new FcmSendResultTestBuilder().outcome(FcmSendOutcome.RETRYABLE_FAILURE)
                    .successCount(0).retryableFailureCount(1).errorMessage("provider-error").build();

            assertThat(fcm.outcome()).isEqualTo(FcmSendOutcome.RETRYABLE_FAILURE);
            assertThat(fcm.successCount()).isZero();
            assertThat(fcm.retryableFailureCount()).isEqualTo(1);
            assertThat(fcm.errorMessage()).isEqualTo("provider-error");
            assertThatThrownBy(() -> new FcmSendResultTestBuilder().targetCount(2).build())
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new FcmSendResultTestBuilder().targetCount(-1).build())
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new FcmSendResultTestBuilder().outcome(null).build())
                    .isInstanceOf(NullPointerException.class);
            assertThat(new AuthEmailSendResultTestBuilder().outcome(null).providerMessageId(null).build().outcome()).isNull();
            assertThat(new NotificationSendResultTestBuilder().errorMessage(" ").build().errorMessage()).isEqualTo(" ");
        }

        @Test
        void whenBuildingDirectPairShouldUseProductionFactoryAndFreshDefaultGraph() {
            var builder = new DirectConversationPairTestBuilder();
            var first = builder.build();
            var second = builder.build();

            assertThat(first.getId()).isNull();
            assertThat(second.getConversation()).isNotSameAs(first.getConversation());
            var conversation = ConversationTestBuilder.firstDirectConversation().build();
            assertThat(builder.conversation(conversation).build().getConversation()).isSameAs(conversation);
            assertThatThrownBy(() -> builder.firstUserId(null).build()).isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("Mutable fixture isolation tests:")
    class MutableFixtureIsolationTests {
        @Test
        void whenProvidingDeliveriesShouldCopyContainersButKeepEntityReferencesAndNullElements() {
            var delivery = new NotificationDeliveryTestBuilder().build();
            var input = new HashSet<com.mazurek.eventOrganizer.notification.domain.NotificationDelivery>();
            input.add(delivery);
            input.add(null);
            var builder = new NotificationTestBuilder().deliveries(input);
            input.clear();
            var first = builder.build();
            var second = builder.build();

            assertThat(first.getDeliveries()).isNotSameAs(second.getDeliveries()).containsExactlyInAnyOrder(delivery, null);
            assertThat(requirePresent(first.getDeliveries().stream().filter(d -> d != null).findFirst(), "Expected the explicit delivery override to be retained")).isSameAs(delivery);
            first.getDeliveries().clear();
            assertThat(second.getDeliveries()).containsExactlyInAnyOrder(delivery, null);
            assertThat(builder.build().getDeliveries()).containsExactlyInAnyOrder(delivery, null);
            assertThat(builder.deliveries(null).build().getDeliveries()).isNull();
            var defaults = new NotificationTestBuilder();
            defaults.build().getDeliveries().add(delivery);
            assertThat(defaults.build().getDeliveries()).isEmpty();
        }

        @Test
        void whenProvidingRolesAndTagsShouldPreserveCopiesReferencesAndExplicitNull() {
            var role = RoleTestBuilder.userRole().build();
            var roles = new HashSet<>(Set.of(role));
            var users = new UserTestBuilder().roles(roles);
            roles.clear();
            var firstUser = users.build();
            var secondUser = users.build();
            assertThat(firstUser.getRoles().iterator().next()).isSameAs(role);
            assertThat(firstUser.getRoles()).isNotSameAs(secondUser.getRoles());
            firstUser.getRoles().clear();
            assertThat(secondUser.getRoles()).containsExactly(role);
            assertThat(users.roles(null).build().getRoles()).isNull();

            var tags = new HashSet<>(TagConstants.DEFAULT_EVENT_TAGS);
            var events = new EventCreateDtoTestBuilder().tags(tags);
            tags.clear();
            var firstEvent = events.build();
            var secondEvent = events.build();
            firstEvent.getTags().clear();
            assertThat(secondEvent.getTags()).containsExactlyInAnyOrderElementsOf(TagConstants.DEFAULT_EVENT_TAGS);
            assertThat(events.tags(null).build().getTags()).isNull();
        }

        @Test
        void whenProvidingMultipartBytesShouldCopyInputAndEachBuiltBuffer() throws Exception {
            byte[] input = TestFileContentFactory.jpg();
            assertThat(input).isNotEmpty();
            var builder = MultipartFileTestBuilder.jpgFile().content(input);
            input[0] ^= 1;
            var first = builder.buildMultipartFile();
            var second = builder.buildMultipartFile();
            assertThat(first.getBytes()).isNotSameAs(second.getBytes()).containsExactly(TestFileContentFactory.jpg());
            first.getBytes()[0] ^= 1;
            assertThat(second.getBytes()).containsExactly(TestFileContentFactory.jpg());
            assertThat(builder.buildMultipartFile().getBytes()).containsExactly(TestFileContentFactory.jpg());
        }

        @Test
        void whenProvidingNullMultipartBytesShouldKeepLibraryNullToEmptyContract() throws Exception {
            var file = MultipartFileTestBuilder.jpgFile().content(null).buildMultipartFile();
            assertThat(file.isEmpty()).isTrue();
            assertThat(file.getBytes()).isEmpty();
        }

        @Test
        void whenBuildingDefaultEntityChildrenShouldNotShareMutableParents() {
            var tokenBuilder = new PasswordResetTokenTestBuilder();
            var firstToken = tokenBuilder.build();
            var secondToken = tokenBuilder.build();
            firstToken.getUser().setEmail("changed@example.com");
            assertThat(secondToken.getUser().getEmail()).isEqualTo(UserConstants.FIRST_USER_EMAIL);

            var deliveryBuilder = new NotificationDeliveryTestBuilder();
            var firstDelivery = deliveryBuilder.build();
            var secondDelivery = deliveryBuilder.build();
            assertThat(secondDelivery.getNotification()).isNotSameAs(firstDelivery.getNotification());
        }

        @Test
        void whenOverridingCollectionsShouldCopyContainersAndPreserveNullElements() {
            var input = new ArrayList<>(List.of(new UpdateNotificationPreferenceDtoTestBuilder().build()));
            var builder = new UpdateNotificationPreferencesDtoTestBuilder().preferences(input);
            input.clear();
            var first = builder.build();
            var second = builder.build();
            first.preferences().clear();
            assertThat(second.preferences()).hasSize(1);

            var withNull = new ArrayList<com.mazurek.eventOrganizer.notification.dto.UpdateNotificationPreferenceDto>();
            withNull.add(null);
            assertThat(builder.preferences(withNull).build().preferences()).containsExactly((com.mazurek.eventOrganizer.notification.dto.UpdateNotificationPreferenceDto) null);
        }

        @Test
        void whenBuildingPropertiesShouldCopyNestedConfigurationAndRetryLists() {
            var retry = new ArrayList<>(PropertyFixtureConstants.RETRY_DELAYS);
            var email = new AuthEmailPropertiesTestBuilder().retryDelays(retry).build();
            var builder = new AuthPropertiesTestBuilder().email(email);
            retry.clear();
            email.getRetryDelays().clear();
            var first = builder.build();
            var second = builder.build();
            first.getEmail().getRetryDelays().clear();

            assertThat(second.getEmail().getRetryDelays()).containsExactlyElementsOf(PropertyFixtureConstants.RETRY_DELAYS);
            assertThat(new AuthPropertiesTestBuilder().email(null).rateLimit(null).build().getEmail()).isNull();
            assertThat(new AuthEmailPropertiesTestBuilder().retryDelays(null).pollDelay(null).build().getRetryDelays()).isNull();
            assertThat(new AuthEmailPropertiesTestBuilder().pollDelay(Duration.ofSeconds(-1)).build().getPollDelay())
                    .isEqualTo(Duration.ofSeconds(-1));
        }

        @Test
        void whenOverridingEncryptionKeysShouldCopyMapAndValuesWithoutReplacingNull() {
            var key = new EncryptionKeyTestBuilder().build();
            var keys = new LinkedHashMap<String, com.mazurek.eventOrganizer.config.properties.EncryptionProperties.EncryptionKey>();
            keys.put("custom", key);
            keys.put("missing", null);
            var builder = new EncryptionPropertiesTestBuilder().messageKeys(keys);
            keys.clear();
            key.setSalt("changed");
            var first = builder.build();
            var second = builder.build();
            first.getMessageKeys().get("custom").setSalt("changed-again");

            assertThat(second.getMessageKeys()).containsKey("missing");
            assertThat(second.getMessageKeys().get("missing")).isNull();
            assertThat(second.getMessageKeys().get("custom").getSalt()).isEqualTo(EncryptionFixtureConstants.SALT);
            assertThat(builder.messageKeys(null).build().getMessageKeys()).isNull();
        }

        @Test
        void whenBuildingSeedReportShouldRespectExplicitRouteAndConstructorInvariant() {
            var builder = new LocalSeedReportTestBuilder();
            assertThat(builder.build().accounts()).isNotSameAs(builder.build().accounts());
            assertThat(new LocalSeedReportResourceTestBuilder().accounts(List.of(UserConstants.FIRST_USER_EMAIL))
                    .protectedRoute(false).build().protectedRoute()).isFalse();
            assertThatThrownBy(() -> new LocalSeedReportAccountTestBuilder().roles(null).build())
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        void whenBuildingFileProjectionShouldSnapshotInputAndIsolateBuffers() {
            byte[] input = TestFileContentFactory.jpg();
            assertThat(input).isNotEmpty();
            var builder = new FileContentProjectionTestBuilder().content(input);
            input[0] = 0;
            var first = builder.build();
            var second = builder.build();
            byte[] leaked = first.getContent();
            assertThat(leaked).isNotEmpty();
            leaked[0] = 0;

            assertThat(first.getContent()).containsExactly(TestFileContentFactory.jpg());
            assertThat(second.getContent()).containsExactly(TestFileContentFactory.jpg());
            assertThat(builder.content(null).contentType(null).build().getContent()).isNull();
            assertThat(new FileOverviewProjectionTestBuilder().ownerId(null).userFileName(" ").build().getUserFileName())
                    .isEqualTo(" ");
        }
    }
}
