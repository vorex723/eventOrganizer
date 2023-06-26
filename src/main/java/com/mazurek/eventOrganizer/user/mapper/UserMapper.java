package com.mazurek.eventOrganizer.user.mapper;

import com.mazurek.eventOrganizer.city.City;
import com.mazurek.eventOrganizer.tag.Tag;
import com.mazurek.eventOrganizer.user.User;
import com.mazurek.eventOrganizer.user.dto.UserWithEventsDto;
import com.mazurek.eventOrganizer.user.dto.UserWithoutEventsDto;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface UserMapper {

    /*@Mapping(source = "id", target = "id")
    @Mapping(source = "firstName", target = "firstName")
    @Mapping(source = "lastName", target = "lastName")
    @Mapping(source = "email", target = "email")
    @Mapping(source = "homeCity", target = "homeCity")*/

    UserWithEventsDto userToUserWithEventsDto(User user);

    UserWithoutEventsDto userToUserWithoutEventDto(User user);

    default String map(Tag tag) {
        if (tag == null)
            return null;
        return tag.getName();
    }

    default String map(City city) {
        if (city == null)
            return null;
        return city.getName();

    }
}
