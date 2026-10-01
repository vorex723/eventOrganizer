package com.mazurek.eventOrganizer.file;

import com.mazurek.eventOrganizer.common.EntityIdentity;
import com.mazurek.eventOrganizer.event.Event;
import com.mazurek.eventOrganizer.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@Builder
@Table(name = "files")
public class File {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String userFileName;
    private String originalFileName;
    private byte[] content;
    private String contentType;
    private Instant uploadDateTime;

    @ManyToOne
    @JoinColumn(name = "event_id")
    private Event event;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User owner;

    public void setEvent(Event newEvent) {
        if (this.event == newEvent)
            return;

        if (this.event != null) {
            this.event.removeFile(this);
        }

        this.event = newEvent;

        if (newEvent != null) {
            newEvent.addFile(this);
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
        File other = (File) o;
        Object identifier = EntityIdentity.identifier(this, this::getId);
        return identifier != null && identifier.equals(EntityIdentity.identifier(other, other::getId));
    }

    @Override
    public final int hashCode() {
        return File.class.hashCode();
    }
}
