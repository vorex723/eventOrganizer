package com.mazurek.eventOrganizer.testData.builders;

import com.mazurek.eventOrganizer.thread.Thread;
import com.mazurek.eventOrganizer.threadReply.ThreadReply;
import com.mazurek.eventOrganizer.user.User;

import java.time.Instant;
import java.util.UUID;

import static com.mazurek.eventOrganizer.testData.TestConstants.*;

/**
 * Builder for creating ThreadReply test objects with sensible defaults.
 */
public class ThreadReplyTestBuilder {

    private UUID id = ThreadReplyConstants.FIRST_REPLY_ID;
    private Thread thread = ThreadTestBuilder.firstThread().build();
    private User replier = UserTestBuilder.firstUser().build();
    private String replierNameAtCreation = UserConstants.FIRST_USER_FULL_NAME;
    private String content = ThreadReplyConstants.FIRST_REPLY_CONTENT;
    private Instant replyDate = TimeConstants.NOW;
    private Instant lastUpdate = TimeConstants.NOW;
    private Integer editCounter = ThreadReplyConstants.INITIAL_EDIT_COUNTER;

    public static ThreadReplyTestBuilder firstReply() {
        return new ThreadReplyTestBuilder()
                .id(ThreadReplyConstants.FIRST_REPLY_ID)
                .content(ThreadReplyConstants.FIRST_REPLY_CONTENT)
                .replier(UserTestBuilder.firstUser().build())
                .replierNameAtCreation(UserConstants.FIRST_USER_FULL_NAME)
                .replyDate(TimeConstants.ONE_HOUR_AGO);
    }

    public static ThreadReplyTestBuilder secondReply() {
        return new ThreadReplyTestBuilder()
                .id(ThreadReplyConstants.SECOND_REPLY_ID)
                .content(ThreadReplyConstants.SECOND_REPLY_CONTENT)
                .replier(UserTestBuilder.secondUser().build())
                .replierNameAtCreation(UserConstants.SECOND_USER_FULL_NAME)
                .replyDate(TimeConstants.ONE_HOUR_AGO);
    }

    public static ThreadReplyTestBuilder thirdReply() {
        return new ThreadReplyTestBuilder()
                .id(ThreadReplyConstants.THIRD_REPLY_ID)
                .content(ThreadReplyConstants.THIRD_REPLY_CONTENT)
                .replier(UserTestBuilder.thirdUser().build())
                .replierNameAtCreation(UserConstants.THIRD_USER_FULL_NAME)
                .replyDate(TimeConstants.ONE_HOUR_AGO);
    }

    public static ThreadReplyTestBuilder oldReply() {
        return new ThreadReplyTestBuilder()
                .id(ThreadReplyConstants.OLD_REPLY_ID)
                .content(ThreadReplyConstants.OLD_REPLY_CONTENT)
                .replyDate(TimeConstants.SEVEN_HOURS_AGO); // Beyond 6 hour edit window
    }

    public ThreadReplyTestBuilder id(UUID id) {
        this.id = id;
        return this;
    }

    public ThreadReplyTestBuilder thread(Thread thread) {
        this.thread = thread;
        return this;
    }

    public ThreadReplyTestBuilder replier(User replier) {
        this.replier = replier;
        this.replierNameAtCreation = replier != null ? replier.getFullName() : null;
        return this;
    }

    public ThreadReplyTestBuilder replierNameAtCreation(String replierNameAtCreation) {
        this.replierNameAtCreation = replierNameAtCreation;
        return this;
    }

    public ThreadReplyTestBuilder content(String content) {
        this.content = content;
        return this;
    }

    public ThreadReplyTestBuilder replyDate(Instant replyDate) {
        this.replyDate = replyDate;
        this.lastUpdate = replyDate;
        return this;
    }

    public ThreadReplyTestBuilder lastUpdate(Instant lastUpdate) {
        this.lastUpdate = lastUpdate;
        return this;
    }

    public ThreadReplyTestBuilder editCounter(Integer editCounter) {
        this.editCounter = editCounter;
        return this;
    }

    public ThreadReply build() {
        ThreadReply threadReply = ThreadReply.builder()
                .id(id)
                .thread(thread)
                .replier(replier)
                .replierNameAtCreation(replierNameAtCreation)
                .content(content)
                .replyDate(replyDate)
                .lastUpdate(lastUpdate)
                .editCounter(editCounter)
                .build();

        this.thread.addReplyToThread(threadReply);
        this.replier.addThreadReply(threadReply);

        return threadReply;
    }
}
