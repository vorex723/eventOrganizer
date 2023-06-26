package com.mazurek.eventOrganizer.user;

import com.mazurek.eventOrganizer.exception.*;
import com.mazurek.eventOrganizer.user.dto.ChangeUserDetailsDto;
import com.mazurek.eventOrganizer.user.dto.ChangeUserEmailDto;
import com.mazurek.eventOrganizer.user.dto.ChangeUserPasswordDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;

@Controller
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable("id") Long id){
        try{
            return ResponseEntity.ok(userService.getUserById(id));
        }
        catch (UserNotFoundException e){
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/update")
    public ResponseEntity<?> changeUserDetails(
             @RequestBody ChangeUserDetailsDto changeUserDetailsDto,
             @RequestHeader("Authorization") String jwt)
    {
        try{
           return ResponseEntity.ok().body(userService.changeUserDetails(changeUserDetailsDto, jwt.substring(7)));
        } catch (RuntimeException e){
            return ResponseEntity.badRequest().build();
        }
        //return null;
    }
    @PutMapping("/change-password")
    public ResponseEntity<?> changePassword(
            @RequestBody ChangeUserPasswordDto changeUserPasswordDto,
            @RequestHeader("Authorization") String jwt)
    {

        try {
            return ResponseEntity.ok().body(userService.changeUserPassword(changeUserPasswordDto,jwt.substring(7)));
        } catch (InvalidUserException | InvalidPasswordException | NotMatchingPasswordsException e){
            return ResponseEntity.badRequest().body(Collections.singletonMap("Message", e.getMessage()));
        }
    }

    @PutMapping("/change-email")
    public ResponseEntity<?> changeEmail(
            @RequestBody ChangeUserEmailDto changeUserEmailDto,
            @RequestHeader("Authorization") String jwt)
    {
        try{
            return ResponseEntity.ok().body(userService.changeUserEmail(changeUserEmailDto,jwt.substring(7)));
        } catch (InvalidEmailException | InvalidPasswordException exception){
            return ResponseEntity.badRequest().body(Collections.singletonMap("Message", exception.getMessage()));
        }
    }

}
