package com.mazurek.eventOrganizer.conversation.mapper;

import com.mazurek.eventOrganizer.conversation.Conversation;
import com.mazurek.eventOrganizer.conversation.Message;
import com.mazurek.eventOrganizer.conversation.dto.ConversationDto;
import com.mazurek.eventOrganizer.conversation.dto.MessageDto;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface ConversationMapper {
    ConversationDto mapConversationToConversationDto(Conversation conversation);

    default MessageDto messageToMessageDto(Message message){
        return Mappers.getMapper(MessageMapper.class).mapMessageToMessageDto(message);
    }
}
