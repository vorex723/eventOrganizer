package com.mazurek.eventOrganizer.tag;

import com.mazurek.eventOrganizer.event.Event;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Getter
@Setter
@AllArgsConstructor
@Builder
@Table(name = "tags")
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    @ManyToMany(mappedBy = "tags", cascade = CascadeType.ALL)
    private Set<Event> events = new HashSet<>();
    public Tag() {
        //events = new HashSet<>();
    }
    public Tag(String name) {
        //events = new HashSet<>();
        this.name = name;
    }
    public void addEvent(Event event){
        if(events.contains(event))
            return;
        events.add(event);
    }

    public void removeEvent(Event event){
        if(!events.contains(event))
            return;
        events.remove(event);
    }
}
