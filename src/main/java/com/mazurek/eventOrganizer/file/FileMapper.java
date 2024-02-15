package com.mazurek.eventOrganizer.file;

import com.mazurek.eventOrganizer.user.User;
import com.mazurek.eventOrganizer.user.dto.UserWithoutEventsDto;
import com.mazurek.eventOrganizer.user.mapper.UserMapper;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface FileMapper {

    FileDto mapFileToFileDto(File file);

    default UserWithoutEventsDto userToUserWithoutEventsDto(User user){
        return Mappers.getMapper(UserMapper.class).mapUserToUserWithoutEventDto(user);
    }
}
