package com.mazurek.eventOrganizer.testData.builders;

import com.mazurek.eventOrganizer.user.Role;

import static com.mazurek.eventOrganizer.testData.TestConstants.*;

/**
 * Builder for creating Role test objects with sensible defaults.
 */
public class RoleTestBuilder {

    private Long id = RoleConstants.USER_ROLE_ID;
    private String name = RoleConstants.ROLE_USER_NAME;

    public static RoleTestBuilder userRole() {
        return new RoleTestBuilder()
                .id(RoleConstants.USER_ROLE_ID)
                .name(RoleConstants.ROLE_USER_NAME);
    }

    public static RoleTestBuilder adminRole() {
        return new RoleTestBuilder()
                .id(RoleConstants.ADMIN_ROLE_ID)
                .name(RoleConstants.ROLE_ADMIN_NAME);
    }

    public RoleTestBuilder id(Long id) {
        this.id = id;
        return this;
    }

    public RoleTestBuilder name(String name) {
        this.name = name;
        return this;
    }

    public Role build() {
        Role role = new Role();
        role.setId(id);
        role.setName(name);
        return role;
    }
}