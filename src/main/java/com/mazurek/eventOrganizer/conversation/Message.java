package com.mazurek.eventOrganizer.conversation;

import com.mazurek.eventOrganizer.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.GeneratedColumn;

import java.util.Date;
import java.util.UUID;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "messages")
public class Message {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne
    @JoinColumn(name = "conversation_id")
    private Conversation conversation;
    @CreationTimestamp
    private Date sentDate;
    @ManyToOne
    private User sender;
    private String message;

    public Message(User sender, String message){
        this.sender = sender;
        this.message = message;
    }

    @Override
    public String toString() {
        return "Message{" +
                "id=" + id +
                ", sentDate=" + sentDate +
                ", message='" + message + '\'' +
                '}';
    }
}
