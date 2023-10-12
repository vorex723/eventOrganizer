package com.mazurek.eventOrganizer.event.mapper;

import com.mazurek.eventOrganizer.city.City;
import com.mazurek.eventOrganizer.event.Event;
import com.mazurek.eventOrganizer.event.dto.EventCreationDto;
import com.mazurek.eventOrganizer.event.dto.EventWithUsersDto;
import com.mazurek.eventOrganizer.tag.Tag;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.stereotype.Component;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface EventMapper {


    EventWithUsersDto mapEventToEventWithUsersDto(Event event);

    /*@Mapping(target = "id", ignore = true)
    @Mapping(target = "createDate", ignore = true)
    @Mapping(target = "lastUpdate", ignore = true)
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "attendingUsers", ignore = true)
    //@Mapping(target = "city", )
    Event mapEventCreationDtoToEvent(EventCreationDto eventCreationDto);*/

    default String map(Tag tag) {
        if (tag == null)
            return null;
        return tag.getName();
    }
    default Tag map(String tagName){
        if (tagName == null)
            return null;
        return new Tag(tagName);
    }

    default String map(City city) {
        if (city == null)
            return null;
        return city.getName();

    }

     default City mapCityToString(String city){
        if (city == null || city.isBlank())
            return null;
        return new City(city);
    }

}
