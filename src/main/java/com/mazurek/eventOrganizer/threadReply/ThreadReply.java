package com.mazurek.eventOrganizer.threadReply;

import com.mazurek.eventOrganizer.common.EntityIdentity;
import com.mazurek.eventOrganizer.user.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
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

    @PositiveOrZero
    @Version
    @Column(nullable = false)
    private long version;

    @NotNull
    @ManyToOne(optional = false)
    @JoinColumn(name = "thread_id", nullable = false)
    private com.mazurek.eventOrganizer.thread.Thread thread;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User replier;

    @Size(max = 1000)
    @NotBlank
    @Column(length = 1000, nullable = false)
    private String content;
    @NotNull
    @Column(nullable = false)
    private Instant replyDate;
    @NotNull
    @Column(nullable = false)
    private Instant lastUpdate;

    @PositiveOrZero
    @NotNull
    @Builder.Default
    @Column(nullable = false)
    private Integer editCount = 0;

    public void incrementEditCounter(){
        this.editCount += 1;
    }

    public boolean isReplier(User user){
        return this.replier != null && this.replier.equals(user);
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
