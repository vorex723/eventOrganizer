package com.mazurek.eventOrganizer.tag;

import com.mazurek.eventOrganizer.event.Event;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@Table(name = "tags")
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToMany(mappedBy = "tags", cascade = CascadeType.ALL)
    private List<Event> events;
    public Tag() {
        events = new ArrayList<>();
    }
}
