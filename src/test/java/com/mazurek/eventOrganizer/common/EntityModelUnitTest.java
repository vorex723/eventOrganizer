package com.mazurek.eventOrganizer.common;

import com.mazurek.eventOrganizer.event.Event;
import com.mazurek.eventOrganizer.file.File;
import com.mazurek.eventOrganizer.testData.builders.EventTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.FileTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.ThreadReplyTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.ThreadTestBuilder;
import com.mazurek.eventOrganizer.thread.Thread;
import com.mazurek.eventOrganizer.threadReply.ThreadReply;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Entity model unit tests:")
class EntityModelUnitTest {

    @Nested
    @DisplayName("Association helpers tests:")
    class AssociationsTests {

        @Test
        @DisplayName("When setting thread event should assign owning side without synchronizing collections")
        void whenSettingThreadEventShouldAssignOwningSideWithoutSynchronizingCollections() {
            Event previous = EventTestBuilder.firstEvent().id(null).city(null).owner(null).build();
            Event next = EventTestBuilder.firstEvent().id(null).city(null).owner(null).build();
            Thread thread = ThreadTestBuilder.firstThread().id(null).event(null).owner(null).build();
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
        @DisplayName("When setting file event should assign owning side without synchronizing collections")
        void whenSettingFileEventShouldAssignOwningSideWithoutSynchronizingCollections() {
            Event previous = EventTestBuilder.firstEvent().id(null).city(null).owner(null).build();
            Event next = EventTestBuilder.firstEvent().id(null).city(null).owner(null).build();
            File file = FileTestBuilder.jpgFile().id(null).event(null).owner(null).build();
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
        @DisplayName("When setting reply thread should assign owning side without synchronizing collections")
        void whenSettingReplyThreadShouldAssignOwningSideWithoutSynchronizingCollections() {
            Thread previous = ThreadTestBuilder.firstThread().id(null).event(null).owner(null).build();
            Thread next = ThreadTestBuilder.firstThread().id(null).event(null).owner(null).build();
            ThreadReply reply = ThreadReplyTestBuilder.firstReply().id(null).thread(null).replier(null).build();
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
        @DisplayName("When adding new children should synchronize both sides idempotently")
        void whenAddingNewChildrenShouldSynchronizeBothSidesIdempotently() {
            Event event = EventTestBuilder.firstEvent().id(null).city(null).owner(null).build();
            Thread thread = ThreadTestBuilder.firstThread().id(null).event(null).owner(null).build();
            File file = FileTestBuilder.jpgFile().id(null).event(null).owner(null).build();
            ThreadReply reply = ThreadReplyTestBuilder.firstReply().id(null).thread(null).replier(null).build();

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
        @DisplayName("When removing children should clear owning side idempotently")
        void whenRemovingChildrenShouldClearOwningSideIdempotently() {
            Event event = EventTestBuilder.firstEvent().id(null).city(null).owner(null).build();
            Thread thread = ThreadTestBuilder.firstThread().id(null).event(null).owner(null).build();
            File file = FileTestBuilder.jpgFile().id(null).event(null).owner(null).build();
            ThreadReply reply = ThreadReplyTestBuilder.firstReply().id(null).thread(null).replier(null).build();
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
        @DisplayName("When removing another parents children should not detach them")
        void whenRemovingAnotherParentsChildrenShouldNotDetachThem() {
            Event owner = EventTestBuilder.firstEvent().id(null).city(null).owner(null).build();
            Event unrelated = EventTestBuilder.firstEvent().id(null).city(null).owner(null).build();
            Thread thread = ThreadTestBuilder.firstThread().id(null).event(null).owner(null).build();
            Thread unrelatedThread = ThreadTestBuilder.firstThread().id(null).event(null).owner(null).build();
            File file = FileTestBuilder.jpgFile().id(null).event(null).owner(null).build();
            ThreadReply reply = ThreadReplyTestBuilder.firstReply().id(null).thread(null).replier(null).build();
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
        @DisplayName("When attaching another parents children should reject reparenting")
        void whenAttachingAnotherParentsChildrenShouldRejectReparenting() {
            Event owner = EventTestBuilder.firstEvent().id(null).city(null).owner(null).build();
            Event unrelated = EventTestBuilder.firstEvent().id(null).city(null).owner(null).build();
            Thread thread = ThreadTestBuilder.firstThread().id(null).event(null).owner(null).build();
            Thread unrelatedThread = ThreadTestBuilder.firstThread().id(null).event(null).owner(null).build();
            File file = FileTestBuilder.jpgFile().id(null).event(null).owner(null).build();
            ThreadReply reply = ThreadReplyTestBuilder.firstReply().id(null).thread(null).replier(null).build();
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
        @DisplayName("When adding null children should reject them without changing collections")
        void whenAddingNullChildrenShouldRejectThemWithoutChangingCollections() {
            Event event = EventTestBuilder.firstEvent().id(null).city(null).owner(null).build();
            Thread thread = ThreadTestBuilder.firstThread().id(null).event(null).owner(null).build();

            assertThatThrownBy(() -> event.addThread(null)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> event.addFile(null)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> thread.addReplyToThread(null)).isInstanceOf(NullPointerException.class);

            assertThat(event.getThreads()).isEmpty();
            assertThat(event.getFiles()).isEmpty();
            assertThat(thread.getReplies()).isEmpty();
        }

        @Test
        @DisplayName("When adding existing collection members should synchronize owning side")
        void whenAddingExistingCollectionMembersShouldSynchronizeOwningSide() {
            Event event = EventTestBuilder.firstEvent().id(null).city(null).owner(null).build();
            Thread thread = ThreadTestBuilder.firstThread().id(null).event(null).owner(null).build();
            File file = FileTestBuilder.jpgFile().id(null).event(null).owner(null).build();
            ThreadReply reply = ThreadReplyTestBuilder.firstReply().id(null).thread(null).replier(null).build();
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

    @Nested
    @DisplayName("Entity identity tests:")
    class IdentityTests {

        static Stream<EntityIdentityTestCase> entities() {
            return EntityIdentityTestCase.cases();
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("entities")
        @DisplayName("When comparing entity with itself should be equal")
        void whenComparingEntityWithItselfShouldBeEqual(EntityIdentityTestCase entityCase) {
            Object entity = entityCase.newEntity(null);
            assertThat(entity.equals(entity)).isTrue();

            entityCase.idSetter().accept(entity, UUID.randomUUID());
            assertThat(entity.equals(entity)).isTrue();
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("entities")
        @DisplayName("When comparing transient entities should distinguish them")
        void whenComparingTransientEntitiesShouldDistinguishThem(EntityIdentityTestCase entityCase) {
            Object first = entityCase.newEntity(null);
            Object second = entityCase.newEntity(null);

            assertThat(first.equals(second)).isFalse();
            assertThat(second.equals(first)).isFalse();
            Set<Object> entities = new HashSet<>();
            assertThat(entities.add(first)).isTrue();
            assertThat(entities.add(second)).isTrue();
            assertThat(entities).hasSize(2);
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("entities")
        @DisplayName("When comparing entities with same identifier should be equal")
        void whenComparingEntitiesWithSameIdentifierShouldBeEqual(EntityIdentityTestCase entityCase) {
            UUID id = UUID.randomUUID();
            Object first = entityCase.newEntity(id);
            Object second = entityCase.newEntity(id);
            Object third = entityCase.newEntity(id);

            assertThat(first.equals(second)).isTrue();
            assertThat(second.equals(first)).isTrue();
            assertThat(second.equals(third)).isTrue();
            assertThat(first.equals(third)).isTrue();
            assertThat(first.hashCode()).isEqualTo(second.hashCode()).isEqualTo(third.hashCode());
            Set<Object> entities = new HashSet<>();
            assertThat(entities.add(first)).isTrue();
            assertThat(entities.add(second)).isFalse();
            assertThat(entities).hasSize(1);
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("entities")
        @DisplayName("When comparing entities with different or missing identifiers should distinguish them")
        void whenComparingEntitiesWithDifferentOrMissingIdentifiersShouldDistinguishThem(EntityIdentityTestCase entityCase) {
            Object persisted = entityCase.newEntity(UUID.randomUUID());
            Object other = entityCase.newEntity(UUID.randomUUID());
            Object transientEntity = entityCase.newEntity(null);

            assertThat(persisted.equals(other)).isFalse();
            assertThat(other.equals(persisted)).isFalse();
            assertThat(persisted.equals(transientEntity)).isFalse();
            assertThat(transientEntity.equals(persisted)).isFalse();
            assertThat(persisted.equals(null)).isFalse();
            assertThat(transientEntity.equals(null)).isFalse();
            assertThat(persisted.equals(new Object())).isFalse();
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("entities")
        @DisplayName("When comparing different entity types should distinguish them")
        void whenComparingDifferentEntityTypesShouldDistinguishThem(EntityIdentityTestCase entityCase) {
            UUID id = UUID.randomUUID();
            Object entity = entityCase.newEntity(id);
            EntityIdentityTestCase.cases()
                    .filter(otherCase -> otherCase.entityClass() != entityCase.entityClass())
                    .forEach(otherCase -> {
                        Object other = otherCase.newEntity(id);
                        assertThat(entity.equals(other)).isFalse();
                        assertThat(other.equals(entity)).isFalse();
                    });
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("entities")
        @DisplayName("When assigning identifier should keep entity hash stable")
        void whenAssigningIdentifierShouldKeepEntityHashStable(EntityIdentityTestCase entityCase) {
            Object entity = entityCase.newEntity(null);
            int hashBeforeAssignment = entity.hashCode();
            Set<Object> entities = new HashSet<>();
            entities.add(entity);

            UUID id = UUID.randomUUID();
            entityCase.idSetter().accept(entity, id);
            Object equalEntity = entityCase.newEntity(id);

            assertThat(entity.hashCode()).isEqualTo(hashBeforeAssignment);
            assertThat(entities.contains(entity)).isTrue();
            assertThat(entities.contains(equalEntity)).isTrue();
            assertThat(entities.add(entity)).isFalse();
            assertThat(entities.add(equalEntity)).isFalse();
            assertThat(entities.remove(equalEntity)).isTrue();
            assertThat(entities).isEmpty();
        }
    }

    @Nested
    @DisplayName("Entity field validation tests:")
    class FieldValidationTests {
        private static ValidatorFactory factory;
        private static Validator validator;

        @BeforeAll
        static void initializeValidator() {
            factory = Validation.buildDefaultValidatorFactory();
            validator = factory.getValidator();
        }

        @AfterAll
        static void closeValidator() {
            factory.close();
        }

        static Stream<Arguments> requiredFields() {
            return Stream.of(
                    required(EventTestBuilder.firstEvent()::build, "name"),
                    required(EventTestBuilder.firstEvent()::build, "shortDescription"),
                    required(EventTestBuilder.firstEvent()::build, "longDescription"),
                    required(EventTestBuilder.firstEvent()::build, "exactAddress"),
                    required(EventTestBuilder.firstEvent()::build, "createDate"),
                    required(EventTestBuilder.firstEvent()::build, "lastUpdate"),
                    required(EventTestBuilder.firstEvent()::build, "eventStartDate"),
                    required(EventTestBuilder.firstEvent()::build, "city"),
                    required(ThreadTestBuilder.firstThread()::build, "event"),
                    required(ThreadTestBuilder.firstThread()::build, "name"),
                    required(ThreadTestBuilder.firstThread()::build, "content"),
                    required(ThreadTestBuilder.firstThread()::build, "createDate"),
                    required(ThreadTestBuilder.firstThread()::build, "lastUpdate"),
                    required(ThreadTestBuilder.firstThread()::build, "lastActivity"),
                    required(ThreadTestBuilder.firstThread()::build, "editCount"),
                    required(ThreadTestBuilder.firstThread()::build, "replyCount"),
                    required(ThreadReplyTestBuilder.firstReply()::build, "thread"),
                    required(ThreadReplyTestBuilder.firstReply()::build, "content"),
                    required(ThreadReplyTestBuilder.firstReply()::build, "replyDate"),
                    required(ThreadReplyTestBuilder.firstReply()::build, "lastUpdate"),
                    required(ThreadReplyTestBuilder.firstReply()::build, "editCount"),
                    required(FileTestBuilder.jpgFile()::build, "event"),
                    required(FileTestBuilder.jpgFile()::build, "userFileName"),
                    required(FileTestBuilder.jpgFile()::build, "originalFileName"),
                    required(FileTestBuilder.jpgFile()::build, "contentType"),
                    required(FileTestBuilder.jpgFile()::build, "content"),
                    required(FileTestBuilder.jpgFile()::build, "uploadDateTime")
            );
        }

        private static Arguments required(Supplier<?> supplier, String field) {
            return Arguments.of(supplier, field);
        }

        @ParameterizedTest(name = "{0}: required field {1}")
        @MethodSource("requiredFields")
        void whenRequiredValuesAreMissingShouldRejectThem(Supplier<?> supplier, String field) throws Exception {
            Object entity = supplier.get();
            setField(entity, field, null);
            assertThat(validator.validate(entity)).anyMatch(violation -> violation.getPropertyPath().toString().equals(field));
        }

        static Stream<Arguments> invalidValues() {
            return Stream.of(
                    invalid(EventTestBuilder.firstEvent()::build, "name", " \t\n"),
                    invalid(EventTestBuilder.firstEvent()::build, "name", "x".repeat(256)),
                    invalid(EventTestBuilder.firstEvent()::build, "attendeeCount", -1),
                    invalid(EventTestBuilder.firstEvent()::build, "maxAttendees", 0),
                    invalid(ThreadTestBuilder.firstThread()::build, "editCount", -1),
                    invalid(ThreadTestBuilder.firstThread()::build, "replyCount", -1),
                    invalid(ThreadTestBuilder.firstThread()::build, "version", -1L),
                    invalid(ThreadTestBuilder.firstThread()::build, "content", "x".repeat(1001)),
                    invalid(ThreadReplyTestBuilder.firstReply()::build, "editCount", -1),
                    invalid(ThreadReplyTestBuilder.firstReply()::build, "version", -1L),
                    invalid(ThreadReplyTestBuilder.firstReply()::build, "content", " \t"),
                    invalid(FileTestBuilder.jpgFile()::build, "contentType", "")
            );
        }

        private static Arguments invalid(Supplier<?> supplier, String field, Object value) {
            return Arguments.of(supplier, field, value);
        }

        @ParameterizedTest(name = "{0}: invalid value of {1}")
        @MethodSource("invalidValues")
        void whenValuesAreBlankOversizedOrNegativeShouldRejectThem(Supplier<?> supplier, String field, Object value) throws Exception {
            Object entity = supplier.get();
            setField(entity, field, value);
            assertThat(validator.validate(entity)).anyMatch(violation -> violation.getPropertyPath().toString().equals(field));
        }

        @Test
        void whenAuthorsAreAnonymousOrCapacityIsUnlimitedShouldAcceptValues() {
            Event event = EventTestBuilder.firstEvent().owner(null).maxAttendees(null).build();
            Thread thread = ThreadTestBuilder.firstThread().owner(null).build();
            ThreadReply reply = ThreadReplyTestBuilder.firstReply().replier(null).build();
            File file = FileTestBuilder.jpgFile().owner(null).build();
            assertThat(validator.validate(event)).isEmpty();
            assertThat(validator.validate(thread)).isEmpty();
            assertThat(validator.validate(reply)).isEmpty();
            assertThat(validator.validate(file)).isEmpty();
        }

        private static void setField(Object entity, String fieldName, Object value) throws Exception {
            var field = entity.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(entity, value);
        }
    }
}
