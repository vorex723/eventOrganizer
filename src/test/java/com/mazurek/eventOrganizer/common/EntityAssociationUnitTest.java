package com.mazurek.eventOrganizer.common;

import com.mazurek.eventOrganizer.event.Event;
import com.mazurek.eventOrganizer.file.File;
import com.mazurek.eventOrganizer.thread.Thread;
import com.mazurek.eventOrganizer.threadReply.ThreadReply;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Entity association helper tests:")
class EntityAssociationUnitTest {

    @Test
    @DisplayName("Thread setter should only assign the owning-side reference")
    void shouldSetThreadEventWithoutSynchronizingCollections() {
        Event previous = new Event();
        Event next = new Event();
        Thread thread = new Thread();
        previous.addThread(thread);

        thread.setEvent(next);

        assertThat(thread.getEvent()).isSameAs(next);
        assertThat(previous.getThreads()).containsExactly(thread);
        assertThat(next.getThreads()).isEmpty();
        thread.setEvent(null);
        assertThat(thread.getEvent()).isNull();
        assertThat(previous.getThreads()).containsExactly(thread);
    }

    @Test
    @DisplayName("File setter should only assign the owning-side reference")
    void shouldSetFileEventWithoutSynchronizingCollections() {
        Event previous = new Event();
        Event next = new Event();
        File file = new File();
        previous.addFile(file);

        file.setEvent(next);

        assertThat(file.getEvent()).isSameAs(next);
        assertThat(previous.getFiles()).containsExactly(file);
        assertThat(next.getFiles()).isEmpty();
        file.setEvent(null);
        assertThat(file.getEvent()).isNull();
        assertThat(previous.getFiles()).containsExactly(file);
    }

    @Test
    @DisplayName("Reply setter should only assign the owning-side reference")
    void shouldSetReplyThreadWithoutSynchronizingCollections() {
        Thread previous = new Thread();
        Thread next = new Thread();
        ThreadReply reply = new ThreadReply();
        previous.addReplyToThread(reply);

        reply.setThread(next);

        assertThat(reply.getThread()).isSameAs(next);
        assertThat(previous.getReplies()).containsExactly(reply);
        assertThat(next.getReplies()).isEmpty();
        reply.setThread(null);
        assertThat(reply.getThread()).isNull();
        assertThat(previous.getReplies()).containsExactly(reply);
    }

    @Test
    @DisplayName("Add helpers should synchronize new children and remain idempotent")
    void shouldAddNewChildrenAndSynchronizeBothSides() {
        Event event = new Event();
        Thread thread = new Thread();
        File file = new File();
        ThreadReply reply = new ThreadReply();

        event.addThread(thread);
        event.addThread(thread);
        event.addFile(file);
        event.addFile(file);
        thread.addReplyToThread(reply);
        thread.addReplyToThread(reply);

        assertThat(event.getThreads()).containsExactly(thread);
        assertThat(thread.getEvent()).isSameAs(event);
        assertThat(event.getFiles()).containsExactly(file);
        assertThat(file.getEvent()).isSameAs(event);
        assertThat(thread.getReplies()).containsExactly(reply);
        assertThat(reply.getThread()).isSameAs(thread);
    }

    @Test
    @DisplayName("Remove helpers should synchronize both sides and remain idempotent")
    void shouldRemoveChildrenAndClearOwningSide() {
        Event event = new Event();
        Thread thread = new Thread();
        File file = new File();
        ThreadReply reply = new ThreadReply();
        event.addThread(thread);
        event.addFile(file);
        thread.addReplyToThread(reply);

        thread.removeReply(reply);
        thread.removeReply(reply);
        event.removeThread(thread);
        event.removeThread(thread);
        event.removeFile(file);
        event.removeFile(file);

        assertThat(event.getThreads()).isEmpty();
        assertThat(thread.getEvent()).isNull();
        assertThat(event.getFiles()).isEmpty();
        assertThat(file.getEvent()).isNull();
        assertThat(thread.getReplies()).isEmpty();
        assertThat(reply.getThread()).isNull();
    }

    @Test
    @DisplayName("Removing a child absent from this parent should not detach it from another parent")
    void shouldNotDetachAnotherParentsChildren() {
        Event owner = new Event();
        Event unrelated = new Event();
        Thread thread = new Thread();
        Thread unrelatedThread = new Thread();
        File file = new File();
        ThreadReply reply = new ThreadReply();
        owner.addThread(thread);
        owner.addFile(file);
        thread.addReplyToThread(reply);

        unrelated.removeThread(thread);
        unrelated.removeFile(file);
        unrelatedThread.removeReply(reply);

        assertThat(thread.getEvent()).isSameAs(owner);
        assertThat(file.getEvent()).isSameAs(owner);
        assertThat(reply.getThread()).isSameAs(thread);
        assertThat(owner.getThreads()).containsExactly(thread);
        assertThat(owner.getFiles()).containsExactly(file);
        assertThat(thread.getReplies()).containsExactly(reply);
    }

    @Test
    @DisplayName("Add helpers should reject implicit reparenting without changing either side")
    void shouldRejectAttachingAnotherParentsChildren() {
        Event owner = new Event();
        Event unrelated = new Event();
        Thread thread = new Thread();
        Thread unrelatedThread = new Thread();
        File file = new File();
        ThreadReply reply = new ThreadReply();
        owner.addThread(thread);
        owner.addFile(file);
        thread.addReplyToThread(reply);

        assertThatThrownBy(() -> unrelated.addThread(thread)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> unrelated.addFile(file)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> unrelatedThread.addReplyToThread(reply)).isInstanceOf(IllegalArgumentException.class);

        assertThat(unrelated.getThreads()).isEmpty();
        assertThat(unrelated.getFiles()).isEmpty();
        assertThat(unrelatedThread.getReplies()).isEmpty();
        assertThat(thread.getEvent()).isSameAs(owner);
        assertThat(file.getEvent()).isSameAs(owner);
        assertThat(reply.getThread()).isSameAs(thread);
        assertThat(owner.getThreads()).containsExactly(thread);
        assertThat(owner.getFiles()).containsExactly(file);
        assertThat(thread.getReplies()).containsExactly(reply);
    }

    @Test
    @DisplayName("Add helpers should reject null children without modifying collections")
    void shouldRejectNullChildren() {
        Event event = new Event();
        Thread thread = new Thread();

        assertThatThrownBy(() -> event.addThread(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> event.addFile(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> thread.addReplyToThread(null)).isInstanceOf(NullPointerException.class);

        assertThat(event.getThreads()).isEmpty();
        assertThat(event.getFiles()).isEmpty();
        assertThat(thread.getReplies()).isEmpty();
    }

    @Test
    @DisplayName("Add helpers should assign owning-side references even if collection membership already exists")
    void shouldSynchronizeExistingCollectionMembers() {
        Event event = new Event();
        Thread thread = new Thread();
        File file = new File();
        ThreadReply reply = new ThreadReply();
        event.getThreads().add(thread);
        event.getFiles().add(file);
        thread.getReplies().add(reply);

        event.addThread(thread);
        event.addFile(file);
        thread.addReplyToThread(reply);

        assertThat(thread.getEvent()).isSameAs(event);
        assertThat(file.getEvent()).isSameAs(event);
        assertThat(reply.getThread()).isSameAs(thread);
        assertThat(event.getThreads()).containsExactly(thread);
        assertThat(event.getFiles()).containsExactly(file);
        assertThat(thread.getReplies()).containsExactly(reply);
    }
}
