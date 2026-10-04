package com.mazurek.eventOrganizer.thread;

import com.mazurek.eventOrganizer.common.EntityIdentity;
import com.mazurek.eventOrganizer.event.Event;
import com.mazurek.eventOrganizer.threadReply.ThreadReply;
import com.mazurek.eventOrganizer.user.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.NotBlank;
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
@Table(name = "threads")
public class Thread {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @PositiveOrZero
    @Version
    @Column(nullable = false)
    private long version;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Size(max = 255)
    @NotBlank
    @Column(nullable = false, length = 255)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User owner;
    @Size(max = 1000)
    @NotBlank
    @NotNull
    @Column(nullable = false, length = 1000)
    private String content;
    @NotNull
    @Column(nullable = false)
    private Instant createDate;
    @NotNull
    @Column(nullable = false)
    private Instant lastUpdate;
    @NotNull
    @Column(nullable = false)
    private Instant lastActivity;
    @PositiveOrZero
    @NotNull
    @Builder.Default
    @Column(nullable = false)
    private Integer editCount = 0;
    @PositiveOrZero
    @NotNull
    @Builder.Default
    @Column(nullable = false)
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
        Objects.requireNonNull(reply, "reply");
        if (reply.getThread() != null && !equals(reply.getThread())) {
            throw new IllegalArgumentException("Reply already belongs to another thread");
        }
        this.replies.add(reply);
        reply.setThread(this);
    }

    public void removeReply(ThreadReply reply) {
        if (this.replies.remove(reply) && equals(reply.getThread())) {
            reply.setThread(null);
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
