package com.mazurek.eventOrganizer.user;

import com.mazurek.eventOrganizer.user.dto.UserWithEventsDto;
import lombok.*;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Getter
@Setter
@RequiredArgsConstructor
@Service
public class UserServiceImpl implements UserService{

    private final UserRepository userRepository;

    @Override
    public UserWithEventsDto getUserById(Long id) {
        Optional<User> userOptional = userRepository.findById(id);


        return null;
    }
}
