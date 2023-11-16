package com.mazurek.eventOrganizer.event.dto;

import com.mazurek.eventOrganizer.city.City;
import com.mazurek.eventOrganizer.tag.Tag;
import com.mazurek.eventOrganizer.user.User;
import com.mazurek.eventOrganizer.user.dto.UserWithoutEventsDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EventWithUsersDto {

    private Long id;
    private String name;
    private String shortDescription;
    private String longDescription;
    private String city;
    private String exactAddress;
    private List<String> tags = new ArrayList<>();
    private UserWithoutEventsDto owner;
    private List<UserWithoutEventsDto> attendingUsers = new ArrayList<>();
    private LocalDateTime eventStartDate;
    private Date createDate;
    private Date lastUpdate;


}
