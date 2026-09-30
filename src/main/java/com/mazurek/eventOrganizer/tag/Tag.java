package com.mazurek.eventOrganizer.tag;

import com.mazurek.eventOrganizer.common.EntityIdentity;
import com.mazurek.eventOrganizer.event.Event;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.*;
import java.util.Locale;

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
        this.name = name.toLowerCase(Locale.ROOT).trim();
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

    @Override
    public final boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || EntityIdentity.persistentClass(this) != EntityIdentity.persistentClass(o)) {
            return false;
        }
        Tag other = (Tag) o;
        Object identifier = EntityIdentity.identifier(this, this::getId);
        return identifier != null && identifier.equals(EntityIdentity.identifier(other, other::getId));
    }

    @Override
    public final int hashCode() {
        return Tag.class.hashCode();
    }
}
