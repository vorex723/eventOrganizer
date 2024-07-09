package com.mazurek.eventOrganizer.notification;

import com.mazurek.eventOrganizer.notification.dto.NotificationDto;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface NotificationMapper {

    @Mapping(source = "createDate", target = "createDate")
    NotificationDto mapNotificationToNotificationDto(Notification notification);
}
