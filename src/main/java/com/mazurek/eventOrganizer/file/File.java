package com.mazurek.eventOrganizer.file;

import com.mazurek.eventOrganizer.event.Event;
import com.mazurek.eventOrganizer.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.Objects;
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

    private String ownerNameAtCreation;

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

    public void setOwner(User newOwner) {
        if (this.owner == newOwner)
            return;

        if (this.owner != null) {
            this.owner.removeFile(this);
        }

        this.owner = newOwner;

        if (newOwner != null) {
            newOwner.addFile(this);
        }
    }

    public String getOwnerDisplayName() {
        if (owner != null) {
            return owner.getFullName();
        }
        return ownerNameAtCreation != null ? ownerNameAtCreation + " (deleted)" : "Unknown User";
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        File file = (File) o;
        return Objects.equals(id, file.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}