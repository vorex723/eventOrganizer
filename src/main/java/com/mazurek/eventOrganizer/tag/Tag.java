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
    @ManyToMany(mappedBy = "tags",fetch = FetchType.LAZY)
    private Set<Event> events = new HashSet<>();
    public Tag() {
        events = new HashSet<>();
    }
    public Tag(String name) {
        events = new HashSet<>();
        this.name = name.toLowerCase();
    }
    public void addEvent(Event event) {
        if(this.events.contains(event))
            return;
        this.events.add(event);
        if (!event.getTags().contains(this)) {
            event.addTag(this);
        }
    }

    public void removeEvent(Event event) {
        if(!this.events.contains(event))
            return;
        this.events.remove(event);
        if (event.getTags().contains(this)) {
            event.removeTag(this);
        }
    }

    public boolean containsEvent(Event event){
        return this.events.contains(event);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Tag tag = (Tag) o;
        return Objects.equals(id, tag.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
