package com.mazurek.eventOrganizer.thread;

import com.mazurek.eventOrganizer.common.EntityIdentity;
import com.mazurek.eventOrganizer.event.Event;
import com.mazurek.eventOrganizer.threadReply.ThreadReply;
import com.mazurek.eventOrganizer.user.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
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

    @Version
    private long version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id")
    private Event event;

    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User owner;
    @NotNull
    @Column(nullable = false, length = 1000)
    private String content;
    @NotNull
    private Instant createDate;
    @NotNull
    private Instant lastUpdate;
    @NotNull
    private Instant lastActivity;
    @Builder.Default
    private Integer editCount = 0;
    @Builder.Default
    private Integer replyCount = 0;

    @Builder.Default
    @OneToMany(mappedBy = "thread", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<ThreadReply> replies = new HashSet<>();

    public boolean isUserOwner(User user){
        return this.owner != null && this.owner.equals(user);
    }

    public void incrementEditCounter(){
        this.editCount += 1;
    }
    public void addReplyToThread(ThreadReply reply){
        this.replies.add(reply);

        if (!reply.getThread().equals(this))
            reply.setThread(this);
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


    @Override
    public final boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || EntityIdentity.persistentClass(this) != EntityIdentity.persistentClass(o)) {
            return false;
        }
        Thread other = (Thread) o;
        Object identifier = EntityIdentity.identifier(this, this::getId);
        return identifier != null && identifier.equals(EntityIdentity.identifier(other, other::getId));
    }

    @Override
    public final int hashCode() {
        return Thread.class.hashCode();
    }
}
