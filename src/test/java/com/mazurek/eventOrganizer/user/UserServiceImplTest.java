package com.mazurek.eventOrganizer.user;

import com.mazurek.eventOrganizer.city.City;
import com.mazurek.eventOrganizer.event.Event;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserServiceImpl userService = new UserServiceImpl(userRepository);
    private BCryptPasswordEncoder passwordEncoder= new BCryptPasswordEncoder();

    @BeforeEach
    void setUp() {
        Optional <User> userOptional = Optional.of(
                new User().builder()
                        .id(1L)
                        .email("example@dot.com")
                        .role(Role.USER)
                        .homeCity(new City(1L,"Rzeszow",new ArrayList<>(), new HashSet<>()))
                        .attendingEvents(new ArrayList<>())
                        .userEvents(new ArrayList<>())
                        .password(passwordEncoder.encode("examplePassword"))
                        .build()
        );
    }


}