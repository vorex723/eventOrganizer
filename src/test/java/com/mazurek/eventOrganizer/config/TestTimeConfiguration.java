package com.mazurek.eventOrganizer.config;

import com.mazurek.eventOrganizer.testData.TestConstants;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import java.time.Clock;

@Configuration
@Profile("test")
public class TestTimeConfiguration {

    @Bean
    @Primary
    public Clock fixedApplicationClock() {
        return TestConstants.TimeConstants.FIXED_CLOCK;
    }
}
