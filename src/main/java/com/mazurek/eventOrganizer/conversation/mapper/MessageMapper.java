package com.mazurek.eventOrganizer.conversation.mapper;

import com.mazurek.eventOrganizer.conversation.Message;
import com.mazurek.eventOrganizer.conversation.dto.MessageDto;
import com.mazurek.eventOrganizer.user.User;
import com.mazurek.eventOrganizer.user.dto.UserWithoutEventsDto;
import com.mazurek.eventOrganizer.user.mapper.UserMapper;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface MessageMapper {

   // @Mapping(source = "sentDate", target = "sentDate")
    MessageDto mapMessageToMessageDto(Message message);

    default UserWithoutEventsDto userToUserWithoutEventsDto(User user){
        return Mappers.getMapper(UserMapper.class).mapUserToUserWithoutEventDto(user);
    }
}
