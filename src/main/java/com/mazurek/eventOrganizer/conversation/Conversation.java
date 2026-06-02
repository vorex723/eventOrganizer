package com.mazurek.eventOrganizer.conversation;

import com.mazurek.eventOrganizer.conversation.participant.ConversationParticipant;
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
@Table(name = "conversations")
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConversationType type;
    private String name;
    private Instant createdAt;
    private Instant lastActiveAt;

    @Builder.Default
    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ConversationParticipant> participants = new HashSet<>();

    public void addParticipant(ConversationParticipant participant) {
        this.participants.add(participant);
        participant.setConversation(this);
    }

}