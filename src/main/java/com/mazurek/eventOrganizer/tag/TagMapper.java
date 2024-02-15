package com.mazurek.eventOrganizer.tag;

import com.mazurek.eventOrganizer.event.Event;
import com.mazurek.eventOrganizer.event.dto.EventWithoutUsersDto;
import com.mazurek.eventOrganizer.event.mapper.EventMapper;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface TagMapper {

    //@Mapping(source = "name", target = "name")
    TagDto mapTagToTagDto(Tag tag);

    default EventWithoutUsersDto eventToEventWithoutUsersDto(Event event){
        return Mappers.getMapper(EventMapper.class).mapEventToEventWithoutUsersDto(event);
    }
}
