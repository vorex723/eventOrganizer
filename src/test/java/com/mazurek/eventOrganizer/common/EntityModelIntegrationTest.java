package com.mazurek.eventOrganizer.common;

import com.mazurek.eventOrganizer.city.City;
import com.mazurek.eventOrganizer.event.Event;
import com.mazurek.eventOrganizer.file.File;
import com.mazurek.eventOrganizer.jwt.JwtUserDetails;
import com.mazurek.eventOrganizer.tag.Tag;
import com.mazurek.eventOrganizer.testData.builders.EventTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.FileTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.ThreadReplyTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.ThreadTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.UserTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.TagTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.JwtUserDetailsTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.dto.ThreadCreateDtoTestBuilder;
import com.mazurek.eventOrganizer.thread.Thread;
import com.mazurek.eventOrganizer.thread.ThreadRepository;
import com.mazurek.eventOrganizer.thread.ThreadService;
import com.mazurek.eventOrganizer.thread.dto.ThreadDto;
import com.mazurek.eventOrganizer.threadReply.ThreadReply;
import com.mazurek.eventOrganizer.user.User;
import com.mazurek.eventOrganizer.testSupport.database.SqlCapture;
import com.mazurek.eventOrganizer.testSupport.database.SqlInspectionConfiguration;
import jakarta.persistence.EntityManager;
import jakarta.validation.ConstraintViolationException;
import org.hibernate.Hibernate;
import org.hibernate.proxy.HibernateProxy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static com.mazurek.eventOrganizer.testData.TestFailureHelper.requirePresent;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Import(SqlInspectionConfiguration.class)
@DisplayName("Entity model persistence and loading tests:")
class EntityModelIntegrationTest {

    @Autowired private EntityManager entityManager;
    @Autowired private SqlCapture sqlCapture;
    @Autowired private ThreadService threadService;
    @Autowired private ThreadRepository threadRepository;

    @org.junit.jupiter.api.BeforeEach
    void resetAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("Association persistence and loading")
    class AssociationsTests {
        @ParameterizedTest(name = "{0} existing threads")
        @ValueSource(ints = {1, 20, 50})
        @DisplayName("When creating thread should not load event threads")
        void whenCreatingThreadShouldNotLoadEventThreads(int existingThreadCount) {
            Graph graph = persistGraph();
            for (int index = 1; index < existingThreadCount; index++) {
                entityManager.persist(ThreadTestBuilder.firstThread().id(null)
                        .name("Existing thread " + index).event(graph.event()).owner(graph.owner()).build());
            }
            flushAndClear();
            Event event = entityManager.find(Event.class, graph.event().getId());
            authenticate(graph.owner());
            assertThat(Hibernate.isInitialized(event.getThreads())).isFalse();
            AtomicReference<ThreadDto> created = new AtomicReference<>();

            List<String> statements = sqlCapture.capture(() -> {
                created.set(threadService.createThreadInEvent(ThreadCreateDtoTestBuilder.firstThread().build(), event.getId()));
                entityManager.flush();
            });

            assertThat(Hibernate.isInitialized(event.getThreads())).isFalse();
            assertThat(Hibernate.isInitialized(event.getFiles())).isFalse();
            Pattern threadRead = Pattern.compile("\\b(?:from|join)\\s+threads\\b", Pattern.CASE_INSENSITIVE);
            assertThat(statements).noneMatch(sql -> threadRead.matcher(sql).find());
            Pattern eventUpdate = Pattern.compile("\\bupdate\\s+events\\b", Pattern.CASE_INSENSITIVE);
            assertThat(statements).noneMatch(sql -> eventUpdate.matcher(sql).find());

            assertThat(threadRepository.findByEventId(event.getId(), PageRequest.of(0, existingThreadCount + 1))
                    .getContent()).hasSize(existingThreadCount + 1)
                    .extracting(Thread::getId).contains(created.get().getId());
            assertThat(Hibernate.isInitialized(event.getThreads())).isFalse();

            entityManager.clear();
            Thread reloaded = entityManager.find(Thread.class, created.get().getId());
            assertThat(reloaded.getEvent().getId()).isEqualTo(event.getId());
            assertThat(reloaded.getOwner().getId()).isEqualTo(graph.owner().getId());
            assertThat(reloaded.getEditCount()).isZero();
            assertThat(reloaded.getReplyCount()).isZero();
            assertThat(entityManager.find(Event.class, event.getId()).getThreads()).hasSize(existingThreadCount + 1);
        }

        @Test
        @DisplayName("When deleting event should cascade to newly created thread")
        void whenDeletingEventShouldCascadeToNewlyCreatedThread() {
            Graph graph = persistGraph();
            entityManager.clear();
            Event event = entityManager.find(Event.class, graph.event().getId());
            assertThat(Hibernate.isInitialized(event.getThreads())).isFalse();
            authenticate(graph.owner());
            ThreadDto created = threadService.createThreadInEvent(ThreadCreateDtoTestBuilder.firstThread().build(), event.getId());
            entityManager.flush();
            assertThat(Hibernate.isInitialized(event.getThreads())).isFalse();

            entityManager.remove(event);
            flushAndClear();

            assertThat(entityManager.find(Event.class, event.getId())).isNull();
            assertThat(entityManager.find(Thread.class, created.getId())).isNull();
            assertThat(entityManager.find(Thread.class, graph.thread().getId())).isNull();
            assertThat(entityManager.find(ThreadReply.class, graph.reply().getId())).isNull();
            assertThat(entityManager.find(File.class, graph.file().getId())).isNull();
            assertThat(entityManager.find(User.class, graph.owner().getId())).isNotNull();
        }

        @Test
        @DisplayName("When removing newly created thread should delete orphan")
        void whenRemovingNewlyCreatedThreadShouldDeleteOrphan() {
            Graph graph = persistGraph();
            entityManager.clear();
            Event event = entityManager.find(Event.class, graph.event().getId());
            authenticate(graph.owner());
            ThreadDto created = threadService.createThreadInEvent(ThreadCreateDtoTestBuilder.firstThread().build(), event.getId());
            entityManager.flush();

            Thread thread = entityManager.find(Thread.class, created.getId());
            event.removeThread(thread);
            flushAndClear();

            assertThat(entityManager.find(Thread.class, created.getId())).isNull();
            assertThat(entityManager.find(Event.class, event.getId()).getThreads())
                    .extracting(Thread::getId).containsExactly(graph.thread().getId());
            assertThat(entityManager.find(ThreadReply.class, graph.reply().getId())).isNotNull();
        }

        private void authenticate(User user) {
            JwtUserDetails principal = new JwtUserDetailsTestBuilder().id(user.getId()).email(user.getEmail())
                    .authorities(List.of()).build();
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        }

        enum Association {
            EVENT_THREAD, EVENT_FILE, THREAD_REPLY
        }

        @ParameterizedTest(name = "{0}")
        @EnumSource(Association.class)
        @DisplayName("When assigning owning side should not read inverse collections")
        void whenAssigningOwningSideShouldNotReadInverseCollections(Association association) {
            Graph originalSource = persistGraph();
            Graph originalTarget = persistGraph();
            entityManager.clear();
            Graph source = reloadGraph(originalSource);
            Graph target = reloadGraph(originalTarget);
            assertInverseCollectionsUninitialized(source);
            assertInverseCollectionsUninitialized(target);

            assertThat(sqlCapture.capture(() -> assignParent(association, source, target))).isEmpty();
            assertAssignedParent(association, source, target);
            assertInverseCollectionsUninitialized(source);
            assertInverseCollectionsUninitialized(target);

            flushAndClear();
            Graph reloadedSource = reloadGraph(originalSource);
            Graph reloadedTarget = reloadGraph(originalTarget);
            assertAssignedParent(association, reloadedSource, reloadedTarget);
            assertInverseCollectionsUninitialized(reloadedSource);
            assertInverseCollectionsUninitialized(reloadedTarget);

            assertThat(sqlCapture.capture(() -> assignParent(association, reloadedSource, null))).isEmpty();
            assertAssignedParent(association, reloadedSource, null);
            assertInverseCollectionsUninitialized(reloadedSource);
            assertInverseCollectionsUninitialized(reloadedTarget);

            // A plain setter is not orphan removal. A surviving child must retain a parent.
            String requiredField = association == Association.THREAD_REPLY ? "thread" : "event";
            assertThatThrownBy(entityManager::flush).isInstanceOf(ConstraintViolationException.class)
                    .satisfies(exception -> assertThat(((ConstraintViolationException) exception).getConstraintViolations())
                            .anyMatch(violation -> violation.getPropertyPath().toString().equals(requiredField)));
        }

        private void assignParent(Association association, Graph childGraph, Graph parentGraph) {
            switch (association) {
                case EVENT_THREAD -> childGraph.thread().setEvent(parentGraph == null ? null : parentGraph.event());
                case EVENT_FILE -> childGraph.file().setEvent(parentGraph == null ? null : parentGraph.event());
                case THREAD_REPLY -> childGraph.reply().setThread(parentGraph == null ? null : parentGraph.thread());
            }
        }

        private void assertAssignedParent(Association association, Graph childGraph, Graph parentGraph) {
            switch (association) {
                case EVENT_THREAD -> assertThat(childGraph.thread().getEvent())
                        .isEqualTo(parentGraph == null ? null : parentGraph.event());
                case EVENT_FILE -> assertThat(childGraph.file().getEvent())
                        .isEqualTo(parentGraph == null ? null : parentGraph.event());
                case THREAD_REPLY -> assertThat(childGraph.reply().getThread())
                        .isEqualTo(parentGraph == null ? null : parentGraph.thread());
            }
        }

        private void assertInverseCollectionsUninitialized(Graph graph) {
            assertThat(Hibernate.isInitialized(graph.event().getThreads())).isFalse();
            assertThat(Hibernate.isInitialized(graph.event().getFiles())).isFalse();
            assertThat(Hibernate.isInitialized(graph.thread().getReplies())).isFalse();
        }

        private Graph reloadGraph(Graph graph) {
            return new Graph(
                    entityManager.find(City.class, graph.city().getId()),
                    entityManager.find(User.class, graph.owner().getId()),
                    entityManager.find(Tag.class, graph.tag().getId()),
                    entityManager.find(Event.class, graph.event().getId()),
                    entityManager.find(Thread.class, graph.thread().getId()),
                    entityManager.find(ThreadReply.class, graph.reply().getId()),
                    entityManager.find(File.class, graph.file().getId()));
        }

        @Test
        @DisplayName("When persisting event without city should reject missing required city")
        void whenPersistingEventWithoutCityShouldRejectMissingRequiredCity() {
            assertThat(entityManager.getMetamodel().entity(Event.class)
                    .getSingularAttribute("city", City.class).isOptional()).isFalse();
            Event event = EventTestBuilder.firstEvent().id(null).owner(null).city(null).build();

            assertThatThrownBy(() -> {
                entityManager.persist(event);
                entityManager.flush();
            }).isInstanceOf(ConstraintViolationException.class)
                    .satisfies(exception -> assertThat(((ConstraintViolationException) exception).getConstraintViolations())
                            .anyMatch(violation -> violation.getPropertyPath().toString().equals("city")));
        }

        @Test
        @DisplayName("When creating event should not read inverse associations")
        void whenCreatingEventShouldNotReadInverseAssociations() {
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
        @DisplayName("When updating event should not read inverse associations")
        void whenUpdatingEventShouldNotReadInverseAssociations() {
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
        @DisplayName("When changing home city should not read inverse associations")
        void whenChangingHomeCityShouldNotReadInverseAssociations() {
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
        @DisplayName("When changing content authors should not read inverse associations")
        void whenChangingContentAuthorsShouldNotReadInverseAssociations() {
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
        @DisplayName("When persisting attendance should persist attendee and count")
        void whenPersistingAttendanceShouldPersistAttendeeAndCount() {
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
        @DisplayName("When removing associated entities should preserve cascading and orphan removal")
        void whenRemovingAssociatedEntitiesShouldPreserveCascadingAndOrphanRemoval(Removal removal) {
            Graph graph = persistGraph();
            entityManager.clear();
            switch (removal) {
                case EVENT -> entityManager.remove(entityManager.find(Event.class, graph.event().getId()));
                case THREAD -> entityManager.remove(entityManager.find(Thread.class, graph.thread().getId()));
                case ORPHAN_THREAD -> {
                    Event event = entityManager.find(Event.class, graph.event().getId());
                    Thread thread = entityManager.find(Thread.class, graph.thread().getId());
                    event.removeThread(thread);
                    assertThat(thread.getEvent()).isNull();
                    assertThat(event.getThreads()).isEmpty();
                }
                case ORPHAN_FILE -> {
                    Event event = entityManager.find(Event.class, graph.event().getId());
                    File file = entityManager.find(File.class, graph.file().getId());
                    event.removeFile(file);
                    assertThat(file.getEvent()).isNull();
                    assertThat(event.getFiles()).isEmpty();
                }
                case ORPHAN_REPLY -> {
                    Thread thread = entityManager.find(Thread.class, graph.thread().getId());
                    ThreadReply reply = entityManager.find(ThreadReply.class, graph.reply().getId());
                    thread.removeReply(reply);
                    assertThat(reply.getThread()).isNull();
                    assertThat(thread.getReplies()).isEmpty();
                }
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
            String cityName = "association city " + UUID.randomUUID();
            City city = com.mazurek.eventOrganizer.testData.builders.CityTestBuilder.warsaw().name(cityName)
                    .externalId(com.mazurek.eventOrganizer.testData.TestCityData.externalId(cityName)).id(null).build();
            entityManager.persist(city);
            User owner = UserTestBuilder.firstUser().id(null).homeCity(city).roles(Set.of())
                    .email("association-" + UUID.randomUUID() + "@example.com").build();
            entityManager.persist(owner);
            Tag tag = TagTestBuilder.firstTag().id(null).name("association tag " + UUID.randomUUID()).build();
            entityManager.persist(tag);
            Event event = EventTestBuilder.firstEvent().id(null).owner(owner).city(city).build();
            event.addTag(tag);
            entityManager.persist(event);
            Thread thread = ThreadTestBuilder.firstThread().id(null).event(null).owner(owner).build();
            event.addThread(thread);
            entityManager.persist(thread);
            ThreadReply reply = ThreadReplyTestBuilder.firstReply().id(null).thread(thread).replier(owner).build();
            thread.addReplyToThread(reply);
            entityManager.persist(reply);
            File file = FileTestBuilder.jpgFile().id(null).event(event).owner(owner).build();
            event.addFile(file);
            entityManager.persist(file);
            entityManager.flush();
            return new Graph(city, owner, tag, event, thread, reply, file);
        }

        private void flushAndClear() {
            entityManager.flush();
            entityManager.clear();
        }

        private record Graph(City city, User owner, Tag tag, Event event, Thread thread, ThreadReply reply, File file) {}
    }

    @Nested
    @DisplayName("Entity identity persistence")
    class IdentityTests {

        static Stream<EntityIdentityTestCase> entities() {
            return EntityIdentityTestCase.cases();
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("entities")
        @DisplayName("When comparing entity with uninitialized or detached proxy should preserve identity")
        void whenComparingEntityWithUninitializedOrDetachedProxyShouldPreserveIdentity(EntityIdentityTestCase entityCase) {
            List<Object> graph = newTransientGraph();
            graph.forEach(entityManager::persist);
            entityManager.flush();
            Object entity = requirePresent(graph.stream()
                    .filter(candidate -> candidate.getClass() == entityCase.entityClass())
                    .findFirst(), "Expected baseline/model prerequisite in whenComparingEntityWithUninitializedOrDetachedProxyShouldPreserveIdentity");
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
        @DisplayName("When persisting and flushing entity should preserve set membership")
        void whenPersistingAndFlushingEntityShouldPreserveSetMembership() {
            List<Object> graph = newTransientGraph();
            EntityIdentityTestCase.cases().forEach(entityCase -> {
                Object entity = requirePresent(graph.stream()
                        .filter(candidate -> candidate.getClass() == entityCase.entityClass())
                        .findFirst(), "Expected baseline/model prerequisite in whenPersistingAndFlushingEntityShouldPreserveSetMembership");
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
            String cityName = "identity city " + UUID.randomUUID();
            City city = com.mazurek.eventOrganizer.testData.builders.CityTestBuilder.warsaw().name(cityName)
                    .externalId(com.mazurek.eventOrganizer.testData.TestCityData.externalId(cityName)).id(null).build();
            User owner = UserTestBuilder.firstUser().id(null).homeCity(city)
                    .email("identity-" + UUID.randomUUID() + "@example.com").roles(Set.of()).build();
            Tag tag = TagTestBuilder.firstTag().id(null).name("identity tag " + UUID.randomUUID()).build();
            Event event = EventTestBuilder.firstEvent().id(null).owner(owner).city(city).build();
            event.addTag(tag);
            Thread thread = ThreadTestBuilder.firstThread().id(null).owner(null).event(null).build();
            thread.setOwner(owner);
            event.addThread(thread);
            ThreadReply reply = ThreadReplyTestBuilder.firstReply().id(null).thread(thread).replier(owner).build();
            File file = FileTestBuilder.jpgFile().id(null).event(event).owner(owner).build();
            thread.addReplyToThread(reply);
            event.addFile(file);
            return List.of(city, owner, tag, event, thread, reply, file);
        }
    }

}
