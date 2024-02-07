package com.mazurek.eventOrganizer.event.mapper;

import com.mazurek.eventOrganizer.city.City;
import com.mazurek.eventOrganizer.event.Event;
import com.mazurek.eventOrganizer.event.dto.EventWithUsersDto;
import com.mazurek.eventOrganizer.event.dto.EventWithoutUsersDto;
import com.mazurek.eventOrganizer.tag.Tag;
import com.mazurek.eventOrganizer.thread.Thread;
import com.mazurek.eventOrganizer.thread.ThreadMapper;
import com.mazurek.eventOrganizer.thread.dto.ThreadShortDto;
import com.mazurek.eventOrganizer.user.User;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface EventMapper {


    EventWithUsersDto mapEventToEventWithUsersDto(Event event);
    @Mapping(source = "attendingUsers", target = "amountOfAttenders")
    EventWithoutUsersDto mapEventToEventWithoutUsersDto(Event event);

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

     default City mapStringToCity(String city){
        if (city == null || city.isBlank())
            return null;
        return new City(city);
    }
    default ThreadShortDto threadToThreadShortDto(Thread thread){
        return Mappers.getMapper(ThreadMapper.class).mapThreadToThreadShortDto(thread);
    }

    default int map(Set<User> attendingUsers){
        return attendingUsers.size();
    }


}
