package com.mazurek.eventOrganizer.tag;

import com.mazurek.eventOrganizer.event.Event;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.*;

@Entity
@Getter
@Setter
@AllArgsConstructor
@Builder
@Table(name = "tags")
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private String name;
    @Builder.Default
    @ManyToMany(mappedBy = "tags", cascade = CascadeType.ALL)
    private Set<Event> events = new HashSet<>();
    public Tag() {
        //events = new HashSet<>();
    }
    public Tag(String name) {
        events = new HashSet<>();
        this.name = name;
    }
    public void addEvent(Event event){
        if(this.events.contains(event))
            return;
        this.events.add(event);
    }

    public void removeEvent(Event event){
        if(!events.contains(event))
            return;
        events.remove(event);
    }
}
