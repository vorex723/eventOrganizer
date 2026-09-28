package com.mazurek.eventOrganizer.conversation.participant;

import com.mazurek.eventOrganizer.conversation.Conversation;
import com.mazurek.eventOrganizer.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"conversation_id", "user_id"}))
public class ConversationParticipant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "user_name_at_join", nullable = false)
    private String userNameAtJoin;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Conversation conversation;

    @Column(nullable = false)
    private Instant joinedAt;

    private Instant leftAt;
    private Instant lastReadAt;
    private Long lastReadMessageId;

    @PrePersist
    void populateUserSnapshot() {
        if (userNameAtJoin == null) {
            userNameAtJoin = user == null ? "Deleted user" : user.getFullName();
        }
    }

}
