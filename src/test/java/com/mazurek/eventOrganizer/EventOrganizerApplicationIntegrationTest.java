package com.mazurek.eventOrganizer;

import com.mazurek.eventOrganizer.auth.email.AuthEmailSender;
import com.mazurek.eventOrganizer.auth.email.TestAuthEmailSender;
import com.mazurek.eventOrganizer.auth.ratelimit.AuthRateLimitStore;
import com.mazurek.eventOrganizer.auth.ratelimit.InMemoryAuthRateLimitStore;
import com.mazurek.eventOrganizer.city.TestCityLookupClient;
import com.mazurek.eventOrganizer.city.cityLookupClient.CityLookupClient;
import com.mazurek.eventOrganizer.notification.delivery.NotificationEmailClient;
import com.mazurek.eventOrganizer.notification.delivery.TestNotificationEmailClient;
import com.mazurek.eventOrganizer.notification.firebaseCloudMessaging.FcmApiClient;
import com.mazurek.eventOrganizer.notification.firebaseCloudMessaging.TestFcmApiClient;
import com.mazurek.eventOrganizer.notification.service.EmailService;
import com.mazurek.eventOrganizer.notification.service.RecordingEmailService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import java.time.Clock;

import static com.mazurek.eventOrganizer.testData.TestConstants.TimeConstants.FIXED_CLOCK;
import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
@DisplayName("EventOrganizerApplication integration tests:")
class EventOrganizerApplicationIntegrationTest {

	@Autowired
	private ApplicationContext context;

	@Test
	@DisplayName("When loading application context should select offline test adapters and fixed clock")
	void whenLoadingApplicationContextShouldSelectOfflineTestAdaptersAndFixedClock() {
		assertThat(context.getEnvironment().getActiveProfiles()).containsExactly("test");
		assertThat(context.getBean(CityLookupClient.class)).isInstanceOf(TestCityLookupClient.class);
		assertThat(context.getBean(AuthEmailSender.class)).isInstanceOf(TestAuthEmailSender.class);
		assertThat(context.getBean(EmailService.class)).isInstanceOf(RecordingEmailService.class);
		assertThat(context.getBean(NotificationEmailClient.class)).isInstanceOf(TestNotificationEmailClient.class);
		assertThat(context.getBean(FcmApiClient.class)).isInstanceOf(TestFcmApiClient.class);
		assertThat(context.getBean(AuthRateLimitStore.class)).isInstanceOf(InMemoryAuthRateLimitStore.class);
		assertThat(context.getBean(Clock.class)).isSameAs(FIXED_CLOCK);
	}

}
