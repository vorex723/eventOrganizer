package com.mazurek.eventOrganizer.tag;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface TagRepository extends JpaRepository<Tag, UUID> {
    Optional<Tag> findByIgnoreCaseName(String name);

    @Modifying
    @Query(value = "INSERT INTO tags (id, name) VALUES (:id, :name) ON CONFLICT ((lower(btrim(name)))) DO NOTHING", nativeQuery = true)
    int insertIfAbsent(@Param("id") UUID id, @Param("name") String name);

    @Query("SELECT COUNT(e) FROM Event e JOIN e.tags t WHERE t.id = :tagId")
    long countEventsByTagId(@Param("tagId") UUID tagId);
}
