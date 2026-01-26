package com.mazurek.eventOrganizer.notification;

import lombok.NoArgsConstructor;

import org.springframework.context.annotation.Profile;

import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Profile("test")
@NoArgsConstructor
public class EmailServiceTestImpl implements EmailService{

        public void sendActivationEmail(String userEmail, UUID tokenID){
        }


}
