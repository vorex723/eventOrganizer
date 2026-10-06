package com.mazurek.eventOrganizer.user;

import com.mazurek.eventOrganizer.testData.builders.UserTestBuilder;
import com.mazurek.eventOrganizer.user.dto.CurrentUserDto;
import com.mazurek.eventOrganizer.user.dto.UserProfileDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UserProfilePrivacy unit tests:")
class UserProfilePrivacyUnitTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void whenSerializingSharedProfileShouldIncludeOnlyIdentityFields() {
        User user = UserTestBuilder.firstUser().build();

        var json = objectMapper.valueToTree(new UserProfileDto(user));

        assertThat(json.size()).isEqualTo(3);
        assertThat(json.path("id").asString()).isEqualTo(user.getId().toString());
        assertThat(json.path("firstName").asString()).isEqualTo(user.getFirstName());
        assertThat(json.path("lastName").asString()).isEqualTo(user.getLastName());
        assertThat(json.has("homeCity")).isFalse();
    }

    @Test
    void whenSerializingDeletedProfileShouldIncludeOnlyIdentityFields() {
        var json = objectMapper.valueToTree(UserProfileDto.deletedUser());

        assertThat(json.size()).isEqualTo(3);
        assertThat(json.path("id").isNull()).isTrue();
        assertThat(json.path("firstName").asString()).isEqualTo("Deleted");
        assertThat(json.path("lastName").asString()).isEqualTo("user");
        assertThat(json.has("homeCity")).isFalse();
    }

    @Test
    void whenSerializingCurrentUserShouldRetainPrivateAccountSettings() {
        User user = UserTestBuilder.firstUser().build();

        var json = objectMapper.valueToTree(new CurrentUserDto(user));

        assertThat(json.size()).isEqualTo(8);
        assertThat(json.path("id").asString()).isEqualTo(user.getId().toString());
        assertThat(json.path("firstName").asString()).isEqualTo(user.getFirstName());
        assertThat(json.path("lastName").asString()).isEqualTo(user.getLastName());
        assertThat(json.path("email").asString()).isEqualTo(user.getEmail());
        assertThat(json.path("homeCity").asString()).isEqualTo(user.getHomeCity().getName());
        assertThat(json.path("homeCityId").asString()).isEqualTo(user.getHomeCity().getId().toString());
        assertThat(json.path("homeCityExternalId").asString()).isEqualTo(user.getHomeCity().getExternalId());
        assertThat(json.path("timeZone").asString()).isEqualTo(user.getTimeZone());
    }
}
