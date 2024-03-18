package com.mazurek.eventOrganizer.conversation;

import com.mazurek.eventOrganizer.user.User;
import jakarta.persistence.*;
import lombok.*;

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

    @Builder.Default
    @ManyToMany(mappedBy = "conversations")
    private Set<User> participants = new HashSet<>();
    @Builder.Default
    @OneToMany(mappedBy = "conversation")
    private List<Message> messages = new ArrayList<>();

    public Conversation(User firstParticipant, User secondParticipant){
        this.participants = new HashSet<>();
        this.messages = new ArrayList<>();
        this.participants.add(firstParticipant);
        this.participants.add(secondParticipant);
    }

    public void addMessage(Message message){
        this.messages.add(message);
        message.setConversation(this);
    }
    public void addParticipant(User user){
        this.participants.add(user);
    }

    @Override
    public String toString() {
        return "Conversation{" +
                "id=" + id +
                ", participants=" + participants +
                ", messages=" + messages +
                '}';
    }
}
