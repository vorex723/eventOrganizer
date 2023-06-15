package com.mazurek.eventOrganizer.user;

import com.mazurek.eventOrganizer.user.dto.UserWithEventsDto;

public interface UserService {
    UserWithEventsDto getUserById(Long id);

}
