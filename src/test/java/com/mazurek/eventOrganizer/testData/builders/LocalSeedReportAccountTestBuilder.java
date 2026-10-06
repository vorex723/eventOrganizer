package com.mazurek.eventOrganizer.testData.builders;

import com.mazurek.eventOrganizer.config.seed.LocalSeedReport.Account;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.mazurek.eventOrganizer.testData.TestConstants.RoleConstants;
import static com.mazurek.eventOrganizer.testData.TestConstants.UserConstants;

/** Constructs data only; explicit overrides are passed through without repair. */
public class LocalSeedReportAccountTestBuilder {
    private UUID id = UserConstants.FIRST_USER_ID;
    private String email = UserConstants.FIRST_USER_EMAIL;
    private List<String> roles = List.of(RoleConstants.ROLE_USER_NAME);

    public LocalSeedReportAccountTestBuilder id(UUID id) {
        this.id = id;
        return this;
    }

    public LocalSeedReportAccountTestBuilder email(String email) {
        this.email = email;
        return this;
    }

    public LocalSeedReportAccountTestBuilder roles(List<String> roles) {
        this.roles = roles == null ? null : new ArrayList<>(roles);
        return this;
    }


    public Account build() {
        return new Account(
                id,
                email,
                roles == null ? null : new ArrayList<>(roles)
        );
    }
}
