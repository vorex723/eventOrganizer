package com.mazurek.eventOrganizer.thread;

import com.mazurek.eventOrganizer.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.Objects;
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

    @ManyToOne
    @JoinColumn(name = "thread_id")
    private Thread thread;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User replier;

    private String replierNameAtCreation;

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

    public String getReplierDisplayName() {
        if (replier != null) {
            return replier.getFullName();
        }
        return replierNameAtCreation != null ? replierNameAtCreation + " (deleted)" : "Unknown User";
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ThreadReply that = (ThreadReply) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}