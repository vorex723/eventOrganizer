package com.mazurek.eventOrganizer.event.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class EventCreationDto {

    @NotBlank(message = "Event name can not be shorter than 5 characters and longer than 50 characters.")
    @Size(min = 5, max = 50, message ="Event name can not be shorter than 5 characters and longer than 50 characters." )
    private String name;
    @NotBlank(message = "Short description of event can not be shorter than 20 characters and longer than 150 characters.")
    @Size(min = 20, max = 250, message = "Short description of event can not be shorter than 20 characters and longer than 150 characters.")
    private String shortDescription;
    @NotBlank(message = "Long description of event can not be shorter than 250 characters and longer than 1500 characters.")
    @Size(min = 250, max = 1500, message = "Long description of event can not be shorter than 250 characters and longer than 1500 characters.")
    private String longDescription;
    @NotBlank(message = "City name can not shorter than 3 characters and longer than 30.")
    @Size(min = 3, max = 30, message = "City name can not shorter than 3 characters and longer than 30.")
    private String city;
    @NotBlank(message = "Exact address can not be shorter than 1 character and longer than 40 characters.")
    @Size(min = 1, max = 40, message = "Exact address can not be shorter than 1 character and longer than 40 characters.")
    private String exactAddress;
    private List<String> tags = new ArrayList<>();
    //@NotEmpty
    private Date eventStartDate;
}

