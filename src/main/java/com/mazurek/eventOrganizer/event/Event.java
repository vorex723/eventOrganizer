package com.mazurek.eventOrganizer.event;

import com.mazurek.eventOrganizer.common.EntityIdentity;
import com.mazurek.eventOrganizer.city.City;
import com.mazurek.eventOrganizer.file.File;
import com.mazurek.eventOrganizer.tag.Tag;
import com.mazurek.eventOrganizer.thread.Thread;
import com.mazurek.eventOrganizer.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.*;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "events")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private String name;
    private String shortDescription;


    @Column(columnDefinition = "text")
    private String longDescription;
    private Instant createDate;
    private Instant lastUpdate;
    private Instant eventStartDate;
    private Integer maxAttendees;
    private String timeZoneId;

    @ManyToOne
    @JoinColumn(name = "city_id")
    private City city;
    private String exactAddress;
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User owner;

    @Builder.Default
    @ManyToMany
    @JoinTable(name = "event_user", joinColumns = @JoinColumn(name = "event_id"), inverseJoinColumns = @JoinColumn(name = "user_id"))
    private Set<User> attendees = new HashSet<>();

    @Builder.Default
    @Column(nullable = false)
    private int attendeeCount = 0;

    @Builder.Default
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "event_tag", joinColumns = @JoinColumn(name = "event_id"), inverseJoinColumns = @JoinColumn(name = "tag_id"))
    private Set<Tag> tags = new HashSet<>();

    @Builder.Default
    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<Thread> threads = new HashSet<>();

    @Builder.Default
    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<File> files = new HashSet<>();

    public void addAttendee(User user) {
        if (attendees.contains(user))
            return;
        attendees.add(user);
        attendeeCount++;
    }

    public void removeAttendee(User user) {
        if (!attendees.contains(user))
            return;
        attendees.remove(user);
        attendeeCount--;
    }

    public void setTags(Set<Tag> newTags) {
        Set<Tag> incoming = newTags != null ? new HashSet<>(newTags) : Collections.emptySet();

        this.tags.retainAll(incoming);
        this.tags.addAll(incoming);
    }

    public void addTag(Tag tag) {
        this.tags.add(tag);
    }

    public void removeTag(Tag tag) {
        this.tags.remove(tag);
    }

    public boolean isUserAttendeeOrOwner(User user) {
        return this.attendees.contains(user) || (this.owner != null && this.owner.equals(user));
    }

    public void addThread(Thread thread) {
        if (this.threads.contains(thread))
            return;
        this.threads.add(thread);
        if (!thread.getEvent().equals(this))
            thread.setEvent(this);
    }

    public void addFile(File file) {
        this.files.add(file);
    }

    public void removeFile(File file) {
        this.files.remove(file);
    }

    public boolean hadPlace(Instant now) {
        return eventStartDate.isBefore(now);
    }

    public boolean containsThread(Thread thread) {
        return this.threads.contains(thread);
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || EntityIdentity.persistentClass(this) != EntityIdentity.persistentClass(o)) {
            return false;
        }
        Event other = (Event) o;
        Object identifier = EntityIdentity.identifier(this, this::getId);
        return identifier != null && identifier.equals(EntityIdentity.identifier(other, other::getId));
    }

    @Override
    public final int hashCode() {
        return Event.class.hashCode();
    }


}
