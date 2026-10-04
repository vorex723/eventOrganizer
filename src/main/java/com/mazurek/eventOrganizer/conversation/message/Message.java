package com.mazurek.eventOrganizer.conversation.message;

import com.mazurek.eventOrganizer.conversation.Conversation;
import com.mazurek.eventOrganizer.user.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;
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
            @Index(name = "idx_message_conversation_created", columnList = "conversation_id, sent_date DESC, id DESC")
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

    @NotNull
    @ManyToOne(optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @NotNull
    @Column(name = "sent_date", nullable = false)
    private Instant sentDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id")
    private User sender;

    @Size(max = 255)
    @NotBlank
    @Column(name = "sender_name_at_creation", nullable = false, length = 255)
    private String senderNameAtCreation;

    @Size(max = 100)
    @NotBlank
    @Column(name = "encryption_key_id", nullable = false, length = 100)
    private String encryptionKeyId;

    @NotBlank
    @Column(nullable = false, columnDefinition = "text")
    private String content;

    public Message(User sender, String senderNameAtCreation, String encryptionKeyId, String content, Instant sentDate, Conversation conversation) {
        this.sender = sender;
        this.senderNameAtCreation = senderNameAtCreation;
        this.encryptionKeyId = encryptionKeyId;
        this.content = content;
        this.sentDate = sentDate;
        this.conversation = conversation;
    }

    @PrePersist
    void populateSenderSnapshot() {
        if (senderNameAtCreation == null) {
            senderNameAtCreation = sender == null ? "Deleted user" : sender.getFullName();
        }
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
