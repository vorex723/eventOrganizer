package com.mazurek.eventOrganizer.conversation.message;

import com.mazurek.eventOrganizer.conversation.Conversation;
import com.mazurek.eventOrganizer.user.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "messages",
    indexes = {
            @Index(name = "idx_message_conversation_created", columnList = "conversation_id, sent_date DESC"),
            @Index(name = "idx_message_conversation_id", columnList = "conversation_id, id")
    })
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "message_id_seq")
    @SequenceGenerator(
            name = "message_id_seq",
            sequenceName = "message_id_seq",
            allocationSize = 50
    )
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @NotNull
    @Column(name = "sent_date", nullable = false)
    private Instant sentDate;

    @ManyToOne
    private User sender;
    @Column(nullable = false, length = 3000)
    private String content;

    public Message(User sender, String content, Instant sentDate, Conversation conversation) {
        this.sender = sender;
        this.content = content;
        this.sentDate = sentDate;
        this.conversation = conversation;
    }

    @Override
    public String toString() {
        return "Message{" +
                "id=" + id +
                ", sentDate=" + sentDate +
                ", message='" + content + '\'' +
                '}';
    }
}
