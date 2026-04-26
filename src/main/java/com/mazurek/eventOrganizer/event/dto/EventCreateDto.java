package com.mazurek.eventOrganizer.event.dto;

import com.mazurek.eventOrganizer.validators.MinFutureDateOffset;
import com.mazurek.eventOrganizer.validators.ValidTimeZone;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class EventCreateDto {

    @NotBlank(message = "Event name cannot be shorter than 5 characters and longer than 50 characters.")
    @Size(min = 5, max = 50, message = "Event name cannot be shorter than 5 characters and longer than 50 characters.")
    private String name;
    @NotBlank(message = "Short description of event cannot be shorter than 20 characters and longer than 250 characters.")
    @Size(min = 20, max = 250, message = "Short description of event cannot be shorter than 20 characters and longer than 250 characters.")
    private String shortDescription;
    @NotBlank(message = "Long description of event cannot be shorter than 250 characters and longer than 1500 characters.")
    @Size(min = 250, max = 1500, message = "Long description of event cannot be shorter than 250 characters and longer than 1500 characters.")
    private String longDescription;
    @NotBlank(message = "City name cannot be shorter than 3 characters and longer than 30.")
    @Size(min = 3, max = 30, message = "City name cannot be shorter than 3 characters and longer than 30.")
    private String city;
    @NotBlank(message = "Exact address cannot be shorter than 1 character and longer than 40 characters.")
    @Size(min = 1, max = 40, message = "Exact address cannot be shorter than 1 character and longer than 40 characters.")
    private String exactAddress;
    @NotNull(message = "Tags must be provided.")
    @Builder.Default
    private Set<@NotBlank(message = "Tag name cannot be blank.")
    @Size(min = 2, max = 30, message = "Tag name cannot be shorter than 2 characters and longer than 30 characters.") String> tags = new HashSet<>();

    @NotNull(message = "You have to specify event start date and time. It has to exceed at least 48 hours from time of creation.")
    @MinFutureDateOffset
    private Instant eventStartDate;

    @NotBlank(message = "Time zone must be provided.")
    @Size(min = 3, max = 35, message = "Time zone cannot be shorter than 3 and longer than 35 characters.")
    @ValidTimeZone
    private String timeZone;

}
