package com.mazurek.eventOrganizer.thread;

import com.mazurek.eventOrganizer.event.Event;
import com.mazurek.eventOrganizer.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@Table(name = "threads")
public class Thread {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id")
    private Event event;

    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User owner;

    private String ownerNameAtCreation;

    private String content;
    private Instant createDate;
    private Instant lastUpdate;
    private Integer editCounter;


    @Builder.Default
    @OneToMany(mappedBy = "thread", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<ThreadReply> replies = new HashSet<>();

    public boolean isUserOwner(User user){
        return this.owner != null && this.owner.equals(user);
    }

    public void incrementEditCounter(){
        this.editCounter += 1;
    }

    public void addReplyToThread(ThreadReply reply){
        this.replies.add(reply);

        if (!reply.getThread().equals(this))
            reply.setThread(this);
    }

    public boolean containsReply(ThreadReply reply){
        return this.replies.contains(reply);
    }

    public void setEvent(Event newEvent) {
        if (this.event == newEvent)
            return;

        if (this.event != null) {
            this.event.getThreads().remove(this);
        }

        this.event = newEvent;

        if (newEvent != null && !newEvent.getThreads().contains(this)) {
            newEvent.addThread(this);
        }
    }

    public void setOwner(User newOwner) {
        if (this.owner == newOwner)
            return;

        if (this.owner != null) {
            this.owner.removeThread(this);
        }

        this.owner = newOwner;

        if (newOwner != null) {
            newOwner.addThread(this);
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
        Thread thread = (Thread) o;
        return Objects.equals(id, thread.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}