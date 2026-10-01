package com.mazurek.eventOrganizer.common;

import com.mazurek.eventOrganizer.city.City;
import com.mazurek.eventOrganizer.event.Event;
import com.mazurek.eventOrganizer.file.File;
import com.mazurek.eventOrganizer.tag.Tag;
import com.mazurek.eventOrganizer.testData.builders.EventTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.FileTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.ThreadReplyTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.ThreadTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.UserTestBuilder;
import com.mazurek.eventOrganizer.thread.Thread;
import com.mazurek.eventOrganizer.threadReply.ThreadReply;
import com.mazurek.eventOrganizer.user.User;
import jakarta.persistence.EntityManager;
import org.hibernate.Hibernate;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Import(EntityAssociationIntegrationTest.SqlInspectionConfiguration.class)
@DisplayName("Entity association persistence and loading tests:")
class EntityAssociationIntegrationTest {

    @Autowired private EntityManager entityManager;
    @Autowired private SqlCapture sqlCapture;

    @Test
    @DisplayName("Assigning existing city, owner and tags should not read their related entities")
    void shouldCreateEventWithoutReadingInverseAssociations() {
        Graph existing = persistGraph();
        entityManager.clear();
        City city = entityManager.find(City.class, existing.city().getId());
        User owner = entityManager.find(User.class, existing.owner().getId());
        Tag tag = entityManager.find(Tag.class, existing.tag().getId());
        Event event = EventTestBuilder.firstEvent().id(null).owner(null).city(null).build();

        assertThat(sqlCapture.capture(() -> {
            event.setCity(city);
            event.setOwner(owner);
            event.setTags(Set.of(tag));
        })).isEmpty();

        entityManager.persist(event);
        flushAndClear();
        Event reloaded = entityManager.find(Event.class, event.getId());
        assertThat(reloaded.getCity().getId()).isEqualTo(city.getId());
        assertThat(reloaded.getOwner().getId()).isEqualTo(owner.getId());
        assertThat(reloaded.getTags()).extracting(Tag::getId).containsExactly(tag.getId());
    }

    @Test
    @DisplayName("Updating event references and tags should not load other events or city residents")
    void shouldUpdateEventWithoutReadingInverseAssociations() {
        Graph first = persistGraph();
        Graph second = persistGraph();
        entityManager.clear();
        Event event = entityManager.find(Event.class, first.event().getId());
        City city = entityManager.find(City.class, second.city().getId());
        User owner = entityManager.find(User.class, second.owner().getId());
        Tag tag = entityManager.find(Tag.class, second.tag().getId());
        Set<Tag> managedTags = event.getTags();
        assertThat(Hibernate.isInitialized(managedTags)).isFalse();

        List<String> statements = sqlCapture.capture(() -> {
            event.setCity(city);
            event.setOwner(owner);
            event.setTags(Set.of(tag));
        });

        // Reading this event's own tags is legitimate; traversing tag.events or city/user collections is not.
        Pattern inverseEntityRead = Pattern.compile("\\b(?:from|join)\\s+(?:events|users)\\b", Pattern.CASE_INSENSITIVE);
        assertThat(statements).noneMatch(sql -> inverseEntityRead.matcher(sql).find());
        assertThat(event.getTags()).isSameAs(managedTags);
        assertThat(Hibernate.isInitialized(event.getThreads())).isFalse();
        assertThat(Hibernate.isInitialized(event.getFiles())).isFalse();

        flushAndClear();
        Event reloaded = entityManager.find(Event.class, event.getId());
        assertThat(reloaded.getCity().getId()).isEqualTo(city.getId());
        assertThat(reloaded.getOwner().getId()).isEqualTo(owner.getId());
        assertThat(reloaded.getTags()).extracting(Tag::getId).containsExactly(tag.getId());
        reloaded.setTags(null);
        flushAndClear();
        assertThat(entityManager.find(Event.class, event.getId()).getTags()).isEmpty();
    }

    @Test
    @DisplayName("Changing home city should persist without reading city residents or events")
    void shouldChangeHomeCityWithoutReadingInverseAssociations() {
        Graph first = persistGraph();
        Graph second = persistGraph();
        entityManager.clear();
        User user = entityManager.find(User.class, first.owner().getId());
        City city = entityManager.find(City.class, second.city().getId());

        assertThat(sqlCapture.capture(() -> user.setHomeCity(city))).isEmpty();

        flushAndClear();
        assertThat(entityManager.find(User.class, user.getId()).getHomeCity().getId()).isEqualTo(city.getId());
    }

    @Test
    @DisplayName("Changing content authors should not read user collections")
    void shouldChangeContentAuthorsWithoutReadingInverseAssociations() {
        Graph first = persistGraph();
        Graph second = persistGraph();
        entityManager.clear();
        Thread thread = entityManager.find(Thread.class, first.thread().getId());
        File file = entityManager.find(File.class, first.file().getId());
        ThreadReply reply = entityManager.find(ThreadReply.class, first.reply().getId());
        User owner = entityManager.find(User.class, second.owner().getId());

        assertThat(sqlCapture.capture(() -> {
            thread.setOwner(owner);
            file.setOwner(owner);
            reply.setReplier(owner);
        })).isEmpty();
        assertThat(Hibernate.isInitialized(thread.getReplies())).isFalse();

        flushAndClear();
        assertThat(entityManager.find(Thread.class, thread.getId()).getOwner().getId()).isEqualTo(owner.getId());
        assertThat(entityManager.find(File.class, file.getId()).getOwner().getId()).isEqualTo(owner.getId());
        assertThat(entityManager.find(ThreadReply.class, reply.getId()).getReplier().getId()).isEqualTo(owner.getId());
    }

    @Test
    @DisplayName("Attendance should persist membership and count without a user inverse collection")
    void shouldPersistAttendanceAndCount() {
        Graph first = persistGraph();
        Graph second = persistGraph();
        entityManager.clear();
        Event event = entityManager.find(Event.class, first.event().getId());
        User attendee = entityManager.find(User.class, second.owner().getId());
        List<String> statements = sqlCapture.capture(() -> {
            event.addAttendee(attendee);
            event.addAttendee(attendee);
        });
        Pattern inverseEventRead = Pattern.compile("\\b(?:from|join)\\s+events\\b", Pattern.CASE_INSENSITIVE);
        assertThat(statements).noneMatch(sql -> inverseEventRead.matcher(sql).find());

        flushAndClear();
        Event reloaded = entityManager.find(Event.class, event.getId());
        assertThat(reloaded.getAttendees()).extracting(User::getId).containsExactly(attendee.getId());
        assertThat(reloaded.getAttendeeCount()).isEqualTo(1);
        User managedAttendee = entityManager.find(User.class, attendee.getId());
        assertThat(sqlCapture.capture(() -> {
            reloaded.removeAttendee(managedAttendee);
            reloaded.removeAttendee(managedAttendee);
        })).isEmpty();

        flushAndClear();
        Event withoutAttendee = entityManager.find(Event.class, event.getId());
        assertThat(withoutAttendee.getAttendees()).isEmpty();
        assertThat(withoutAttendee.getAttendeeCount()).isZero();
    }

    enum Removal {
        EVENT, THREAD, ORPHAN_THREAD, ORPHAN_FILE, ORPHAN_REPLY
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(Removal.class)
    @DisplayName("Retained bidirectional associations should preserve cascading and orphan removal")
    void shouldPreserveCascadingAndOrphanRemoval(Removal removal) {
        Graph graph = persistGraph();
        entityManager.clear();
        switch (removal) {
            case EVENT -> entityManager.remove(entityManager.find(Event.class, graph.event().getId()));
            case THREAD -> entityManager.remove(entityManager.find(Thread.class, graph.thread().getId()));
            case ORPHAN_THREAD -> entityManager.find(Thread.class, graph.thread().getId()).setEvent(null);
            case ORPHAN_FILE -> entityManager.find(File.class, graph.file().getId()).setEvent(null);
            case ORPHAN_REPLY -> entityManager.find(ThreadReply.class, graph.reply().getId()).setThread(null);
        }

        flushAndClear();
        assertThat(entityManager.find(Event.class, graph.event().getId()) != null).isEqualTo(removal != Removal.EVENT);
        boolean threadRemains = removal == Removal.ORPHAN_FILE || removal == Removal.ORPHAN_REPLY;
        assertThat(entityManager.find(Thread.class, graph.thread().getId()) != null).isEqualTo(threadRemains);
        assertThat(entityManager.find(ThreadReply.class, graph.reply().getId()) != null)
                .isEqualTo(removal == Removal.ORPHAN_FILE);
        assertThat(entityManager.find(File.class, graph.file().getId()) != null)
                .isEqualTo(removal != Removal.EVENT && removal != Removal.ORPHAN_FILE);
        assertThat(entityManager.find(User.class, graph.owner().getId())).isNotNull();
        assertThat(entityManager.find(City.class, graph.city().getId())).isNotNull();
        assertThat(entityManager.find(Tag.class, graph.tag().getId())).isNotNull();
    }

    private Graph persistGraph() {
        City city = new City("association city " + UUID.randomUUID());
        entityManager.persist(city);
        User owner = UserTestBuilder.firstUser().id(null).homeCity(city).roles(Set.of())
                .email("association-" + UUID.randomUUID() + "@example.com").build();
        entityManager.persist(owner);
        Tag tag = new Tag("association tag " + UUID.randomUUID());
        entityManager.persist(tag);
        Event event = EventTestBuilder.firstEvent().id(null).owner(owner).city(city).build();
        event.addTag(tag);
        entityManager.persist(event);
        Thread thread = ThreadTestBuilder.firstThread().id(null).event(null).owner(owner).build();
        thread.setEvent(event);
        entityManager.persist(thread);
        ThreadReply reply = ThreadReplyTestBuilder.firstReply().id(null).thread(thread).replier(owner).build();
        entityManager.persist(reply);
        File file = FileTestBuilder.jpgFile().id(null).event(event).owner(owner).build();
        entityManager.persist(file);
        entityManager.flush();
        return new Graph(city, owner, tag, event, thread, reply, file);
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    private record Graph(City city, User owner, Tag tag, Event event, Thread thread, ThreadReply reply, File file) {}

    @TestConfiguration(proxyBeanMethods = false)
    static class SqlInspectionConfiguration {
        @Bean
        SqlCapture sqlCapture() {
            return new SqlCapture();
        }

        @Bean
        HibernatePropertiesCustomizer associationSqlInspector(SqlCapture sqlCapture) {
            return properties -> properties.put(AvailableSettings.STATEMENT_INSPECTOR, sqlCapture);
        }
    }

    static class SqlCapture implements StatementInspector {
        private final ThreadLocal<List<String>> capturedStatements = new ThreadLocal<>();

        List<String> capture(Runnable action) {
            List<String> statements = new ArrayList<>();
            capturedStatements.set(statements);
            try {
                action.run();
                return List.copyOf(statements);
            } finally {
                capturedStatements.remove();
            }
        }

        @Override
        public String inspect(String sql) {
            List<String> statements = capturedStatements.get();
            if (statements != null) {
                statements.add(sql);
            }
            return sql;
        }
    }
}
