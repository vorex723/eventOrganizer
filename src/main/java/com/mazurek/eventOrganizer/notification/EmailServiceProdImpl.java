package com.mazurek.eventOrganizer.notification;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Profile({"development","production"})
public class EmailServiceProdImpl implements EmailService{

    private final JavaMailSender javaMailSender;

    public static final String ACTIVATION_URL = "http://localhost:8080/api/v1/auth/activate/";
    public static final String ACTIVATION_EMAIL_BODY = "You can activate your account by opening this link: ";


    public void sendActivationEmail(String userEmail, UUID tokenID){
        String body =
                "<!DOCTYPE html>" +
                        "<html>" +
                        "<body>" +
                        "<p> <b>" + ACTIVATION_EMAIL_BODY + "</b></p>" +
                        "<p>" + "<a href=\"" + ACTIVATION_URL + tokenID + "\"> Activate account. </a>" + "</p>"+
                        "</body>" +
                        "</html>";

        try{
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper messageHelper = new MimeMessageHelper(message, true);

            message.setContent(body, "text/html; charset=utf-8");
            messageHelper.setTo(userEmail);
            messageHelper.setFrom("testowe.andrzej.testowe@gmail.com");
            messageHelper.setSubject("Account activation.");

            javaMailSender.send(message);
        } catch (Exception exception){
            System.out.println(exception.getMessage());
            System.out.println(exception.getCause());
            exception.printStackTrace();
        }

    }
}
