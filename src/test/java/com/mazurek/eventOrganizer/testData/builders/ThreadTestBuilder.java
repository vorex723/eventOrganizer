package com.mazurek.eventOrganizer.testData.builders;

import com.mazurek.eventOrganizer.event.Event;
import com.mazurek.eventOrganizer.thread.Thread;
import com.mazurek.eventOrganizer.user.User;

import java.time.Instant;
import java.util.UUID;

import static com.mazurek.eventOrganizer.testData.TestConstants.*;

public class ThreadTestBuilder {

    private UUID id = ThreadConstants.FIRST_THREAD_ID;
    private String name = ThreadConstants.FIRST_THREAD_NAME;
    private String content = ThreadConstants.FIRST_THREAD_CONTENT;
    private Event event = EventTestBuilder.firstEvent().build();
    private User owner = UserTestBuilder.firstUser().build();
    private String ownerNameAtCreation = UserConstants.FIRST_USER_FULL_NAME;
    private Instant createDate = TimeConstants.NOW;
    private Instant lastUpdate = TimeConstants.NOW;
    private Integer editCounter = ThreadConstants.INITIAL_EDIT_COUNTER;

    public static ThreadTestBuilder firstThread() {
        return new ThreadTestBuilder()
                .id(ThreadConstants.FIRST_THREAD_ID)
                .name(ThreadConstants.FIRST_THREAD_NAME)
                .content(ThreadConstants.FIRST_THREAD_CONTENT)
                .createDate(TimeConstants.ONE_HOUR_AGO);
    }

    public static ThreadTestBuilder secondThread() {
        return new ThreadTestBuilder()
                .id(ThreadConstants.SECOND_THREAD_ID)
                .name(ThreadConstants.SECOND_THREAD_NAME)
                .content(ThreadConstants.SECOND_THREAD_CONTENT)
                .owner(UserTestBuilder.secondUser().build())
                .ownerNameAtCreation(UserConstants.SECOND_USER_FULL_NAME)
                .createDate(TimeConstants.ONE_HOUR_AGO);
    }

    public static ThreadTestBuilder oldThread() {
        return new ThreadTestBuilder()
                .id(ThreadConstants.OLD_THREAD_ID)
                .name(ThreadConstants.OLD_THREAD_NAME)
                .createDate(TimeConstants.SEVEN_HOURS_AGO); // Beyond 6 hour deletion window
    }

    public ThreadTestBuilder id(UUID id) {
        this.id = id;
        return this;
    }

    public ThreadTestBuilder name(String name) {
        this.name = name;
        return this;
    }

    public ThreadTestBuilder content(String content) {
        this.content = content;
        return this;
    }

    public ThreadTestBuilder event(Event event) {
        this.event = event;
        return this;
    }

    public ThreadTestBuilder owner(User owner) {
        this.owner = owner;
        this.ownerNameAtCreation = owner.getFullName();
        return this;
    }

    public ThreadTestBuilder ownerNameAtCreation(String ownerNameAtCreation) {
        this.ownerNameAtCreation = ownerNameAtCreation;
        return this;
    }

    public ThreadTestBuilder createDate(Instant createDate) {
        this.createDate = createDate;
        return this;
    }

    public ThreadTestBuilder lastUpdate(Instant lastUpdate) {
        this.lastUpdate = lastUpdate;
        return this;
    }

    public ThreadTestBuilder editCounter(Integer editCounter) {
        this.editCounter = editCounter;
        return this;
    }

    public Thread build() {
        Thread thread = Thread.builder()
                .id(id)
                .name(name)
                .content(content)
                .event(event)
                .owner(owner)
                .ownerNameAtCreation(ownerNameAtCreation)
                .createDate(createDate)
                .lastUpdate(lastUpdate)
                .editCounter(editCounter)
                .build();

        this.owner.addThread(thread);
        this.event.addThread(thread);

        return thread;
    }
}
