package com.mazurek.eventOrganizer.user;

import com.mazurek.eventOrganizer.testData.builders.UserTestBuilder;
import com.mazurek.eventOrganizer.user.dto.CurrentUserDto;
import com.mazurek.eventOrganizer.user.dto.UserProfileDto;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

class UserProfilePrivacyUnitTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void sharedProfileSerializesOnlyIdentityFields() {
        User user = UserTestBuilder.firstUser().build();

        var json = objectMapper.valueToTree(new UserProfileDto(user));

        assertThat(json.size()).isEqualTo(3);
        assertThat(json.get("id").asText()).isEqualTo(user.getId().toString());
        assertThat(json.get("firstName").asText()).isEqualTo(user.getFirstName());
        assertThat(json.get("lastName").asText()).isEqualTo(user.getLastName());
        assertThat(json.has("homeCity")).isFalse();
    }

    @Test
    void deletedProfileSerializesOnlyIdentityFields() {
        var json = objectMapper.valueToTree(UserProfileDto.deletedUser());

        assertThat(json.size()).isEqualTo(3);
        assertThat(json.get("id").isNull()).isTrue();
        assertThat(json.get("firstName").asText()).isEqualTo("Deleted");
        assertThat(json.get("lastName").asText()).isEqualTo("user");
        assertThat(json.has("homeCity")).isFalse();
    }

    @Test
    void currentUserRetainsHomeCityForAccountSettings() {
        User user = UserTestBuilder.firstUser().build();

        var json = objectMapper.valueToTree(new CurrentUserDto(user));

        assertThat(json.get("homeCity").asText()).isEqualTo(user.getHomeCity().getName());
    }
}
