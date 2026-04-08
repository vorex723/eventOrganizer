package com.mazurek.eventOrganizer.testData.builders;

import com.mazurek.eventOrganizer.auth.dto.AuthenticationRequest;
import com.mazurek.eventOrganizer.testData.TestConstants.*;


public class AuthenticationRequestTestBuilder {

    private String email = UserConstants.FIRST_USER_EMAIL;
    private String password = UserConstants.USER_PASSWORD;

    public static AuthenticationRequestTestBuilder authenticationRequestForFirstUser(){
        return new AuthenticationRequestTestBuilder()
                .email(UserConstants.FIRST_USER_EMAIL)
                .password(UserConstants.USER_PASSWORD);
    }
    public static AuthenticationRequestTestBuilder authenticationRequestForSecondUser(){
        return new AuthenticationRequestTestBuilder()
                .email(UserConstants.SECOND_USER_EMAIL)
                .password(UserConstants.USER_PASSWORD);
    }

    public AuthenticationRequestTestBuilder email(String email){
        this.email = email;
        return this;
    }
    public AuthenticationRequestTestBuilder password(String password){
        this.password = password;
        return this;
    }

    public AuthenticationRequest build(){
        return AuthenticationRequest.builder()
                .email(email)
                .password(password)
                .build();
    }
}
