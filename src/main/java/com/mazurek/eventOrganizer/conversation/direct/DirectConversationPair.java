package com.mazurek.eventOrganizer.conversation.direct;

import com.mazurek.eventOrganizer.conversation.Conversation;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(
        name = "direct_conversation_pair",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_direct_conversation_pair_users",
                columnNames = {"first_user_id","second_user_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DirectConversationPair {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false, unique = true)
    private Conversation conversation;
    @Column(name = "first_user_id", nullable = false)
    private UUID firstUserId;
    @Column(name = "second_user_id", nullable = false)
    private UUID secondUserId;

    private DirectConversationPair(Conversation conversation, UUID firstUserId, UUID secondUserId) {
        this.conversation = conversation;
        this.firstUserId = firstUserId;
        this.secondUserId = secondUserId;
    }

    public static DirectConversationPair of(Conversation conversation, UUID userA, UUID userB) {
        DirectPairIds directPairIds = DirectPairIds.of(userA, userB);

        return new DirectConversationPair(conversation, directPairIds.firstUserId(), directPairIds.secondUserId());
    }

    public record DirectPairIds(UUID firstUserId, UUID secondUserId) {

        public static DirectPairIds of(UUID userA, UUID userB) {
            return userA.toString().compareTo(userB.toString()) < 0
                    ? new DirectPairIds(userA, userB)
                    : new DirectPairIds(userB, userA);
        }
    }
}
