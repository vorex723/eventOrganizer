package com.mazurek.eventOrganizer.threadReply;

import com.mazurek.eventOrganizer.common.EntityIdentity;
import com.mazurek.eventOrganizer.thread.Thread;
import com.mazurek.eventOrganizer.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "thread_replies")
public class ThreadReply {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Version
    private long version;

    @ManyToOne
    @JoinColumn(name = "thread_id")
    private com.mazurek.eventOrganizer.thread.Thread thread;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User replier;

    @Column(length = 1000)
    private String content;
    private Instant replyDate;
    private Instant lastUpdate;

    @Builder.Default
    private Integer editCounter = 0;

    public void incrementEditCounter(){
        this.editCounter += 1;
    }

    public boolean isReplier(User user){
        return this.replier != null && this.replier.equals(user);
    }

    public void setThread(Thread newThread) {
        if (this.thread == newThread)
            return;

        if (this.thread != null) {
            this.thread.getReplies().remove(this);
        }

        this.thread = newThread;

        if (newThread != null && !newThread.getReplies().contains(this)) {
            newThread.addReplyToThread(this);
        }
    }

    public void setReplier(User newReplier) {
        if (this.replier == newReplier)
            return;

        if (this.replier != null) {
            this.replier.removeThreadReply(this);
        }

        this.replier = newReplier;

        if (newReplier != null) {
            newReplier.addThreadReply(this);
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
        ThreadReply other = (ThreadReply) o;
        Object identifier = EntityIdentity.identifier(this, this::getId);
        return identifier != null && identifier.equals(EntityIdentity.identifier(other, other::getId));
    }

    @Override
    public final int hashCode() {
        return ThreadReply.class.hashCode();
    }
}
