package com.mazurek.eventOrganizer.testData.builders.dto;


import com.mazurek.eventOrganizer.auth.dto.RegisterRequest;
import com.mazurek.eventOrganizer.testData.TestConstants.*;

public class RegisterRequestTestBuilder {

    private String firstName = UserConstants.FIRST_USER_FIRST_NAME;
    private String lastName = UserConstants.FIRST_USER_LAST_NAME;
    private String email = UserConstants.FIRST_USER_EMAIL;
    private String emailConfirmation = UserConstants.FIRST_USER_EMAIL;
    private String homeCity = CitiesConstants.WARSAW_NAME;
    private String timeZone = UserConstants.FIRST_USER_TIMEZONE;
    private String password = UserConstants.USER_PASSWORD;
    private String passwordConfirmation = UserConstants.USER_PASSWORD;

    public static RegisterRequestTestBuilder firstUserRegisterRequest(){
        return new RegisterRequestTestBuilder();
    }
    public static RegisterRequestTestBuilder secondUserRegisterRequest(){
        return new RegisterRequestTestBuilder()
                .firstName(UserConstants.SECOND_USER_FIRST_NAME)
                .lastName(UserConstants.SECOND_USER_LAST_NAME)
                .email(UserConstants.SECOND_USER_EMAIL)
                .emailConfirmation(UserConstants.SECOND_USER_EMAIL);
    }
    public static RegisterRequestTestBuilder thirdUserRegisterRequest() {
        return new RegisterRequestTestBuilder()
                .firstName(UserConstants.THIRD_USER_FIRST_NAME)
                .lastName(UserConstants.THIRD_USER_LAST_NAME)
                .email(UserConstants.THIRD_USER_EMAIL)
                .emailConfirmation(UserConstants.THIRD_USER_EMAIL);
    }

    public RegisterRequestTestBuilder firstName(String firstName){
        this.firstName = firstName;
        return this;
    }
    public RegisterRequestTestBuilder lastName(String lastName){
        this.lastName = lastName;
        return this;
    }
    public RegisterRequestTestBuilder email(String email){
        this.email = email;
        return this;
    }
    public RegisterRequestTestBuilder emailConfirmation(String emailConfirmation){
        this.emailConfirmation = emailConfirmation;
        return this;
    }
    public RegisterRequestTestBuilder homeCity(String homeCity){
        this.homeCity = homeCity;
        return this;
    }
    public RegisterRequestTestBuilder timeZone(String timeZone){
        this.timeZone = timeZone;
        return this;
    }
    public RegisterRequestTestBuilder password(String password){
        this.password = password;
        return this;
    }
    public RegisterRequestTestBuilder passwordConfirmation(String passwordConfirmation){
        this.passwordConfirmation = passwordConfirmation;
        return this;
    }


    public RegisterRequest build(){
        return RegisterRequest.builder()
                .firstName(firstName)
                .lastName(lastName)
                .email(email)
                .emailConfirmation(emailConfirmation)
                .homeCity(homeCity)
                .timeZone(timeZone)
                .password(password)
                .passwordConfirmation(passwordConfirmation)
                .build();
    }
}
