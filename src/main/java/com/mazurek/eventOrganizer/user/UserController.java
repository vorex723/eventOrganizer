package com.mazurek.eventOrganizer.user;

import com.mazurek.eventOrganizer.auth.AuthenticationResponse;
import com.mazurek.eventOrganizer.event.EventService;
import com.mazurek.eventOrganizer.event.dto.EventOverviewDto;
import com.mazurek.eventOrganizer.event.dto.EventOverviewPageDto;
import com.mazurek.eventOrganizer.notification.NotificationService;
import com.mazurek.eventOrganizer.notification.dto.NotificationsPageDto;
import com.mazurek.eventOrganizer.user.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final EventService eventService;
    private final NotificationService notificationService;


//**********************************************************************************************************************
//---------------------------------------------------GET----------------------------------------------------------------
// *********************************************************************************************************************
    @GetMapping("/{id}")
    public ResponseEntity<UserProfileDto> getUserById(@PathVariable("id") UUID id){
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @GetMapping("/{id}/events")
    public ResponseEntity<EventOverviewPageDto> getUserEventsByUserId(@PathVariable("id") UUID id,
                                                                      @RequestParam(name = "page", defaultValue = "0", required = false) int pageNumber,
                                                                      @RequestParam(name = "upcoming", defaultValue = "true", required = false) boolean upcomingEventsOnly){
        return ResponseEntity.ok(eventService.getUserEventsByUserId(id,pageNumber , upcomingEventsOnly));
    }

    @GetMapping("/{id}/attending-events")
    public ResponseEntity<EventOverviewPageDto> getUserAttendingEventsByUserId(@PathVariable("id") UUID id,
                                                                               @RequestParam(name = "page", defaultValue = "0", required = false) int pageNumber,
                                                                               @RequestParam(name = "upcoming-event", defaultValue = "false", required = false) boolean upcomingEvents,
                                                                               @RequestHeader("Authorization") String jwt)
    {

        return ResponseEntity.ok(eventService.getUserAttendingEventsByUserId(id, pageNumber, upcomingEvents, jwt.substring(7)));
    }

    @GetMapping("/{userId}/notifications")
    public ResponseEntity<NotificationsPageDto> getUserNotifications(@PathVariable("userId") UUID userId, @RequestHeader("Authorization") String jwtToken, @RequestParam(value = "page", defaultValue = "0", required = false) int page){
        return ResponseEntity.ok().body(notificationService.getUserNotifications(userId, jwtToken.substring(7), page));

    }

    @GetMapping("/{userId}/notifications/{notificationId}")
    public ResponseEntity<?> readNotification(@PathVariable("userId") UUID userId, @PathVariable("notificationId ") UUID notificationId, @RequestHeader("Authorization") String jwtToken){
        notificationService.setNotificationOpened(userId, notificationId, jwtToken.substring(7));
        return ResponseEntity.ok().build();

    }

//**********************************************************************************************************************
//---------------------------------------------------POST---------------------------------------------------------------
// *********************************************************************************************************************
    @PostMapping("/register-token")
    public ResponseEntity<?> registerUserFcmToken(@RequestBody RegisterFcmTokenRequest registerFcmTokenRequest,
                                                  @RequestHeader("Authorization") String jwt)
    {
        if(userService.registerUserFcmToken(registerFcmTokenRequest, jwt.substring(7)))
            return ResponseEntity.ok().body(Collections.singletonMap("result", "true"));
        else
            return ResponseEntity.badRequest().body(Collections.singletonMap("result", "false"));
    }
//**********************************************************************************************************************
//---------------------------------------------------PUT----------------------------------------------------------------
// *********************************************************************************************************************
    @PutMapping("/update")
    public ResponseEntity<UserWithEventsDto> changeUserDetails(
            @Valid @RequestBody ChangeUserDetailsDto changeUserDetailsDto,
            @RequestHeader("Authorization") String jwt)
    {
        return ResponseEntity.ok().body(userService.changeUserDetails(changeUserDetailsDto, jwt.substring(7)));
    }

    @PutMapping("/change-password")
    public ResponseEntity<AuthenticationResponse> changePassword(
            @Valid @RequestBody ChangeUserPasswordDto changeUserPasswordDto,
            @RequestHeader("Authorization") String jwt)
    {
        return ResponseEntity.ok().body(userService.changeUserPassword(changeUserPasswordDto,jwt.substring(7)));

    }
    @PutMapping("/change-email")
    public ResponseEntity<AuthenticationResponse> changeEmail(
            @Valid @RequestBody ChangeUserEmailDto changeUserEmailDto,
            @RequestHeader("Authorization") String jwt)
    {
        return ResponseEntity.ok().body(userService.changeUserEmail(changeUserEmailDto,jwt.substring(7)));
    }

}
