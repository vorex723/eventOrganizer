package com.mazurek.eventOrganizer.common;

import com.mazurek.eventOrganizer.city.City;
import com.mazurek.eventOrganizer.event.Event;
import com.mazurek.eventOrganizer.file.File;
import com.mazurek.eventOrganizer.tag.Tag;
import com.mazurek.eventOrganizer.testData.builders.*;
import com.mazurek.eventOrganizer.thread.Thread;
import com.mazurek.eventOrganizer.threadReply.ThreadReply;
import com.mazurek.eventOrganizer.user.User;
import jakarta.persistence.EntityManager;
import org.hibernate.Hibernate;
import org.hibernate.proxy.HibernateProxy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Entity identity persistence tests:")
class EntityIdentityIntegrationTest {

    @Autowired
    private EntityManager entityManager;

    static Stream<EntityIdentityTestCase> entities() {
        return EntityIdentityTestCase.cases();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("entities")
    @DisplayName("Entity and proxy should share identity without loading the proxy, even when detached")
    void shouldCompareWithUninitializedAndDetachedProxy(EntityIdentityTestCase entityCase) {
        List<Object> graph = newTransientGraph();
        graph.forEach(entityManager::persist);
        entityManager.flush();
        Object entity = graph.stream()
                .filter(candidate -> candidate.getClass() == entityCase.entityClass())
                .findFirst().orElseThrow();
        UUID id = entityCase.idGetter().apply(entity);
        entityManager.clear();

        Object proxy = entityManager.getReference(entityCase.entityClass(), id);
        assertThat(proxy).isInstanceOf(HibernateProxy.class);
        assertThat(Hibernate.isInitialized(proxy)).isFalse();
        assertSameIdentityWithoutLoadingProxy(entity, proxy);

        entityManager.clear();
        assertSameIdentityWithoutLoadingProxy(entity, proxy);
        Object anotherProxy = entityManager.getReference(entityCase.entityClass(), id);
        entityManager.clear();
        assertSameIdentityWithoutLoadingProxy(proxy, anotherProxy);

        Object differentEntity = entityCase.newEntity(UUID.randomUUID());
        Object transientEntity = entityCase.newEntity(null);
        assertThat(proxy.equals(differentEntity)).isFalse();
        assertThat(differentEntity.equals(proxy)).isFalse();
        assertThat(proxy.equals(transientEntity)).isFalse();
        assertThat(transientEntity.equals(proxy)).isFalse();
        assertThat(proxy.equals(null)).isFalse();
        assertThat(proxy.equals(proxy)).isTrue();
        assertThat(Hibernate.isInitialized(proxy)).isFalse();
    }

    private void assertSameIdentityWithoutLoadingProxy(Object entity, Object proxy) {
        assertThat(entity.equals(proxy)).isTrue();
        assertThat(proxy.equals(entity)).isTrue();
        assertThat(proxy.hashCode()).isEqualTo(entity.hashCode());
        Set<Object> entities = new HashSet<>();
        entities.add(entity);
        assertThat(entities.contains(proxy)).isTrue();
        assertThat(entities.add(proxy)).isFalse();
        assertThat(entities.remove(proxy)).isTrue();
        entities.add(proxy);
        assertThat(entities.contains(entity)).isTrue();
        assertThat(entities.add(entity)).isFalse();
        assertThat(entities.remove(entity)).isTrue();
        assertThat(Hibernate.isInitialized(proxy)).isFalse();
        if (entity instanceof HibernateProxy) {
            assertThat(Hibernate.isInitialized(entity)).isFalse();
        }
    }

    @Test
    @DisplayName("Persist and flush should preserve hashes and relationship sets populated before identifier assignment")
    void shouldPreserveSetMembershipThroughPersistAndFlush() {
        List<Object> graph = newTransientGraph();
        EntityIdentityTestCase.cases().forEach(entityCase -> {
            Object entity = graph.stream()
                    .filter(candidate -> candidate.getClass() == entityCase.entityClass())
                    .findFirst().orElseThrow();
            assertThat(entityCase.idGetter().apply(entity)).isNull();
        });
        List<Integer> hashes = graph.stream().map(Object::hashCode).toList();
        Set<Object> entities = new HashSet<>(graph);

        graph.forEach(entityManager::persist);
        entityManager.flush();

        for (int index = 0; index < graph.size(); index++) {
            Object entity = graph.get(index);
            assertThat(entity.hashCode()).isEqualTo(hashes.get(index));
            assertThat(entities.contains(entity)).isTrue();
            assertThat(entities.add(entity)).isFalse();
            assertThat(entities.remove(entity)).isTrue();
        }
        assertThat(entities).isEmpty();

        Event event = (Event) graph.get(3);
        Thread thread = (Thread) graph.get(4);
        ThreadReply reply = (ThreadReply) graph.get(5);
        File file = (File) graph.get(6);

        // Repeated additions must preserve membership in retained bidirectional relationships.
        event.addThread(thread);
        thread.addReplyToThread(reply);
        event.addFile(file);
        assertThat(event.getThreads()).containsExactly(thread);
        assertThat(thread.getReplies()).containsExactly(reply);
        assertThat(event.getFiles()).containsExactly(file);

        assertThat(event.getThreads().remove(thread)).isTrue();
        assertThat(thread.getReplies().remove(reply)).isTrue();
        assertThat(event.getFiles().remove(file)).isTrue();
    }

    private List<Object> newTransientGraph() {
        City city = new City("identity city " + UUID.randomUUID());
        User owner = UserTestBuilder.firstUser().id(null).homeCity(city)
                .email("identity-" + UUID.randomUUID() + "@example.com").roles(Set.of()).build();
        Tag tag = new Tag("identity tag " + UUID.randomUUID());
        Event event = EventTestBuilder.firstEvent().id(null).owner(owner).city(city).build();
        event.addTag(tag);
        Thread thread = ThreadTestBuilder.firstThread().id(null).owner(null).event(null).build();
        thread.setOwner(owner);
        event.addThread(thread);
        ThreadReply reply = ThreadReplyTestBuilder.firstReply().id(null).thread(thread).replier(owner).build();
        File file = FileTestBuilder.jpgFile().id(null).event(event).owner(owner).build();
        return List.of(city, owner, tag, event, thread, reply, file);
    }
}
