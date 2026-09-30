package com.mazurek.eventOrganizer.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Entity identity contract tests:")
class EntityIdentityUnitTest {

    static Stream<EntityIdentityTestCase> entities() {
        return EntityIdentityTestCase.cases();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("entities")
    @DisplayName("An entity should equal itself with or without an identifier")
    void shouldEqualItself(EntityIdentityTestCase entityCase) {
        Object entity = entityCase.newEntity(null);
        assertThat(entity.equals(entity)).isTrue();

        entityCase.idSetter().accept(entity, UUID.randomUUID());
        assertThat(entity.equals(entity)).isTrue();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("entities")
    @DisplayName("Distinct transient entities should remain distinct in a set")
    void shouldDistinguishTransientEntities(EntityIdentityTestCase entityCase) {
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
    @DisplayName("Entities with the same identifier should be symmetrically and transitively equal")
    void shouldEqualEntitiesWithSameIdentifier(EntityIdentityTestCase entityCase) {
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
    @DisplayName("Different or missing identifiers should not be equal")
    void shouldDistinguishDifferentOrMissingIdentifiers(EntityIdentityTestCase entityCase) {
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
    @DisplayName("Different entity types should not be equal even with the same identifier")
    void shouldDistinguishEntityTypes(EntityIdentityTestCase entityCase) {
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
    @DisplayName("Assigning an identifier should preserve set membership and removal by an equal instance")
    void shouldKeepHashStableAfterIdentifierAssignment(EntityIdentityTestCase entityCase) {
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
