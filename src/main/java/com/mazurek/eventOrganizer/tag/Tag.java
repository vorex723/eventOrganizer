package com.mazurek.eventOrganizer.tag;

import com.mazurek.eventOrganizer.event.Event;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private String id;
    private List<Event> events;
    public Tag() {
        events = new ArrayList<>();
    }
}
