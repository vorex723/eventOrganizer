package com.mazurek.eventOrganizer.thread;

import com.mazurek.eventOrganizer.user.User;
import com.mazurek.eventOrganizer.thread.Thread;
import jakarta.persistence.*;
import lombok.*;

import java.util.Calendar;
import java.util.Date;
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

    private String content;
    private Date replayDate;
    private Date lastEditDate;
    private Integer editCounter;

    public void incrementEditCounter(){
        this.editCounter += 1;
        this.lastEditDate = Calendar.getInstance().getTime();
    }
    public boolean isReplier(User user){
        return this.replier.equals(user);
    }

}
