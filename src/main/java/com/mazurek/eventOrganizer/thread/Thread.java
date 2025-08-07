package com.mazurek.eventOrganizer.thread;

import com.mazurek.eventOrganizer.event.Event;
import com.mazurek.eventOrganizer.user.User;
import jakarta.persistence.*;
import jakarta.persistence.Entity;
import lombok.*;

import java.time.ZonedDateTime;
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

    @Getter
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
    private String content;
    private ZonedDateTime createDate;
    private ZonedDateTime lastUpdate;
    private Integer editCounter;

    @Builder.Default
    @OneToMany(mappedBy = "thread", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<ThreadReply> replies = new HashSet<>();

    public boolean isUserOwner(User user){
        return this.owner.equals(user);
    }

    public void incrementEditCounter(){
        this.editCounter += 1;
    }
    public void addReplyToThread(ThreadReply reply){
        this.replies.add(reply);

        if (reply.getThread().equals(this))
            reply.setThread(this);
    }

    public boolean containsReply(ThreadReply reply){
        return this.replies.contains(reply);
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
