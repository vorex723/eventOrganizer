package com.mazurek.eventOrganizer.jwt;

import com.mazurek.eventOrganizer.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetailsService;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JwtUtilTest {

    private JwtUtil jwtUtil;
    private final String SECRET = "secretKeyForJwt";
    private final long JWT_EXPIRATION_TIME = 30000;
    @Mock private UserRepository userRepository;


    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(JWT_EXPIRATION_TIME,SECRET, userRepository);


    }
}