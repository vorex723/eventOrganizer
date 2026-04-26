package com.mazurek.eventOrganizer.testData;

import com.mazurek.eventOrganizer.auth.ActivationToken;
import com.mazurek.eventOrganizer.auth.ActivationTokenRepository;
import com.mazurek.eventOrganizer.auth.AuthenticationService;
import com.mazurek.eventOrganizer.auth.dto.RegisterRequest;
import com.mazurek.eventOrganizer.exception.auth.ActivationTokenNotFoundException;
import com.mazurek.eventOrganizer.exception.user.UserNotFoundException;
import com.mazurek.eventOrganizer.jwt.JwtUserDetails;
import com.mazurek.eventOrganizer.testData.builders.dto.RegisterRequestTestBuilder;
import com.mazurek.eventOrganizer.user.Role;
import com.mazurek.eventOrganizer.user.RoleRepository;
import com.mazurek.eventOrganizer.user.User;
import com.mazurek.eventOrganizer.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import com.mazurek.eventOrganizer.testData.TestConstants.*;

@Component
public class AuthHelper {

    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AuthenticationService authenticationService;
    @Autowired
    private ActivationTokenRepository activationTokenRepository;


    public void setupRolesAndUsers(){
        setupUserRoles();
        registerAndActivateFirstUser();
        registerAndActivateSecondUser();
    }

    private void setupUserRoles(){
        if (roleRepository.findByName(TestConstants.RoleConstants.ROLE_USER_NAME).isEmpty()){
            Role roleUser = new Role(TestConstants.RoleConstants.ROLE_USER_NAME);
            roleRepository.save(roleUser);
        }
        if(roleRepository.findByName(TestConstants.RoleConstants.ROLE_ADMIN_NAME).isEmpty()){
            Role roleAdmin = new Role(TestConstants.RoleConstants.ROLE_ADMIN_NAME);
            roleRepository.save(roleAdmin);
        }
    }

    private void registerAndActivateFirstUser(){
        RegisterRequest firstUserRegisterRequest = RegisterRequestTestBuilder.firstUserRegisterRequest().build();
        authenticationService.register(firstUserRegisterRequest);

        ActivationToken activationToken = activationTokenRepository.findByIgnoreCaseUserEmail(UserConstants.FIRST_USER_EMAIL).orElseThrow(ActivationTokenNotFoundException::new);
        authenticationService.activateAccount(activationToken.getToken());
    }

    private void registerAndActivateSecondUser(){
        RegisterRequest secondUserRegisterRequest = RegisterRequestTestBuilder.secondUserRegisterRequest().build();
        authenticationService.register(secondUserRegisterRequest);

        ActivationToken activationToken = activationTokenRepository.findByIgnoreCaseUserEmail(UserConstants.SECOND_USER_EMAIL).orElseThrow(ActivationTokenNotFoundException::new);
        authenticationService.activateAccount(activationToken.getToken());

    }

    private void setupSecurityContextForUser(String email) {
        SecurityContextHolder.clearContext();
        User user = userRepository.findByIgnoreCaseEmail(email).orElseThrow(UserNotFoundException::new);
        JwtUserDetails userDetails = new JwtUserDetails(
                user.getId(),
                user.getEmail(),
                user.getRoles().stream().map(role -> new SimpleGrantedAuthority(role.getName())).toList());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));
    }

    public void setupSecurityContextForFirstUser() {
        setupSecurityContextForUser(UserConstants.FIRST_USER_EMAIL);
    }

    public void setupSecurityContextForSecondUser() {
        setupSecurityContextForUser(UserConstants.SECOND_USER_EMAIL);
    }

}
