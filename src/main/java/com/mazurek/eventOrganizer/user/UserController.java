package com.mazurek.eventOrganizer.user;

import com.mazurek.eventOrganizer.exception.notification.NotificationNotFoundException;
import com.mazurek.eventOrganizer.exception.user.*;
import com.mazurek.eventOrganizer.notification.NotificationService;
import com.mazurek.eventOrganizer.user.dto.ChangeUserDetailsDto;
import com.mazurek.eventOrganizer.user.dto.ChangeUserEmailDto;
import com.mazurek.eventOrganizer.user.dto.ChangeUserPasswordDto;
import com.mazurek.eventOrganizer.user.dto.RegisterFcmTokenRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

@Controller
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final NotificationService notificationService;

    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable("id") UUID id){
        try{
            return ResponseEntity.ok(userService.getUserById(id));
        }
        catch (UserNotFoundException exception){
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/update")
    public ResponseEntity<?> changeUserDetails(
             @Valid @RequestBody ChangeUserDetailsDto changeUserDetailsDto,
             @RequestHeader("Authorization") String jwt)
    {
        try{
           return ResponseEntity.ok().body(userService.changeUserDetails(changeUserDetailsDto, jwt.substring(7)));
        } catch (RuntimeException exception){
            return ResponseEntity.internalServerError().build();
        }
    }
    @PutMapping("/change-password")
    public ResponseEntity<?> changePassword(
            @Valid @RequestBody ChangeUserPasswordDto changeUserPasswordDto,
            @RequestHeader("Authorization") String jwt)
    {

        try {
            return ResponseEntity.ok().body(userService.changeUserPassword(changeUserPasswordDto,jwt.substring(7)));
        } catch (InvalidUserException | InvalidPasswordException | NotMatchingPasswordsException exception){
            return ResponseEntity.badRequest().body(Collections.singletonMap("Message", exception.getMessage()));
        }
    }

    @PutMapping("/change-email")
    public ResponseEntity<?> changeEmail(
            @Valid @RequestBody ChangeUserEmailDto changeUserEmailDto,
            @RequestHeader("Authorization") String jwt)
    {
        try{
            return ResponseEntity.ok().body(userService.changeUserEmail(changeUserEmailDto,jwt.substring(7)));
        } catch (InvalidEmailException | InvalidPasswordException | UserAlreadyExistException exception ){
            return ResponseEntity.badRequest().body(Collections.singletonMap("Message", exception.getMessage()));
        }
    }

    @PostMapping("/register-token")
    public ResponseEntity<?> registerUserFcmToken(@RequestBody RegisterFcmTokenRequest registerFcmTokenRequest,
                                                  @RequestHeader("Authorization") String jwt)
    {
        if(userService.registerUserFcmToken(registerFcmTokenRequest, jwt.substring(7)))
            return ResponseEntity.ok().body(Collections.singletonMap("result", "true"));
        else
            return ResponseEntity.badRequest().body(Collections.singletonMap("result", "false"));
    }

    @GetMapping("/{userId}/notifications")

    public ResponseEntity<?> getUserNotifications(@PathVariable("userId") UUID userId, @RequestHeader("Authorization") String jwtToken, @RequestParam(value = "page", defaultValue = "0", required = false) int page){
        try{
            return ResponseEntity.ok().body(notificationService.getUserNotifications(userId, jwtToken.substring(7), page));
        }
        catch (InvalidUserException exception){
            return ResponseEntity.badRequest().build();
        }
       /* catch (RuntimeException exception) {
            return ResponseEntity.internalServerError().build();
        }*/
    }

    @GetMapping("/{userId}/notifications/{notificationId}")
    public ResponseEntity<?> readNotification(@PathVariable("userId") UUID userId, @PathVariable("notificationId") UUID notificationId, @RequestHeader("Authorization") String jwtToken){
        try{
            notificationService.setNotificationOpened(userId, notificationId, jwtToken.substring(7));
            return ResponseEntity.ok().build();
        }
        catch (NotificationNotFoundException exception){
            return ResponseEntity.notFound().build();
        }
        catch (RuntimeException exception){
            return ResponseEntity.badRequest().body(Collections.singletonMap("Message",exception.getMessage()));
        }


    }

}
