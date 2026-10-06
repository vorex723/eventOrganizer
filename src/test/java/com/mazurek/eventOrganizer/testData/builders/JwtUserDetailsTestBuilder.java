package com.mazurek.eventOrganizer.testData.builders;

import com.mazurek.eventOrganizer.jwt.JwtUserDetails;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import static com.mazurek.eventOrganizer.testData.TestConstants.RoleConstants;
import static com.mazurek.eventOrganizer.testData.TestConstants.UserConstants;

/** Constructs data only; explicit overrides are passed through without repair. */
public class JwtUserDetailsTestBuilder {
    private UUID id = UserConstants.FIRST_USER_ID;
    private String email = UserConstants.FIRST_USER_EMAIL;
    private Collection<? extends GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(RoleConstants.ROLE_USER_NAME));

    public JwtUserDetailsTestBuilder id(UUID id) {
        this.id = id;
        return this;
    }

    public JwtUserDetailsTestBuilder email(String email) {
        this.email = email;
        return this;
    }

    public JwtUserDetailsTestBuilder authorities(Collection<? extends GrantedAuthority> authorities) {
        this.authorities = authorities == null ? null : new ArrayList<>(authorities);
        return this;
    }


    public JwtUserDetails build() {
        return new JwtUserDetails(
                id,
                email,
                authorities == null ? null : new ArrayList<>(authorities)
        );
    }
}
