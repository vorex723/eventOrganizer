package com.mazurek.eventOrganizer.user;

import com.mazurek.eventOrganizer.auth.dto.AuthenticationResponse;
import com.mazurek.eventOrganizer.event.EventService;
import com.mazurek.eventOrganizer.event.dto.EventOverviewPageDto;
import com.mazurek.eventOrganizer.jwt.DeviceType;
import com.mazurek.eventOrganizer.user.dto.*;
import com.mazurek.eventOrganizer.utils.DeviceTypeResolver;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final EventService eventService;
    private final DeviceTypeResolver deviceTypeResolver;


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

    @GetMapping("/me/attending-events")
    public ResponseEntity<EventOverviewPageDto> getCurrentUserAttendingEvents(
            @RequestParam(name = "page", defaultValue = "0", required = false) int pageNumber,
            @RequestParam(name = "upcoming", defaultValue = "false", required = false) boolean upcomingEvents)
    {
        return ResponseEntity.ok(
                eventService.getCurrentUserAttendingEvents(
                        pageNumber,
                        upcomingEvents));
    }


    @PutMapping("/update")
    public ResponseEntity<UserProfileDto> changeUserDetails(@Valid @RequestBody ChangeUserDetailsDto changeUserDetailsDto)
    {
        return ResponseEntity.ok(userService.changeDetails(changeUserDetailsDto));
    }

    @PutMapping("/change-password")
    public ResponseEntity<AuthenticationResponse> changePassword(
            @Valid @RequestBody ChangeUserPasswordDto changeUserPasswordDto,
            @RequestHeader(value = "X-Device-Type", required = false) String deviceTypeHeader,
            @RequestHeader(value = "User-Agent", required = false) String userAgent)
    {
        DeviceType deviceType = deviceTypeResolver.determineDeviceType(deviceTypeHeader, userAgent);
        return ResponseEntity.ok(userService.changePassword(changeUserPasswordDto,deviceType, userAgent));

    }
    @PutMapping("/change-email")
    public ResponseEntity<AuthenticationResponse> changeEmail(
            @Valid @RequestBody ChangeUserEmailDto changeUserEmailDto,
            @RequestHeader(value = "X-Device-Type", required = false) String deviceTypeHeader,
            @RequestHeader(value = "User-Agent", required = false) String userAgent)
    {
        DeviceType deviceType = deviceTypeResolver.determineDeviceType(deviceTypeHeader, userAgent);
        return ResponseEntity.ok(userService.changeEmail(changeUserEmailDto,deviceType, userAgent));
    }

}
