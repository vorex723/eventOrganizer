package com.mazurek.eventOrganizer.file;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface FileRepository extends JpaRepository<File, UUID> {
    Optional<File> findByIdAndEventId(UUID fileId, UUID eventId);
    Page<File> findByEventId(UUID eventId, Pageable pageable);
    Set<File> findByEventId(UUID eventId);
    Set<File> findByOwnerId(UUID userId);

    long countByEventId(UUID eventId);

    @Query(value = "SELECT COALESCE(SUM(octet_length(content)), 0) FROM files WHERE event_id = :eventId", nativeQuery = true)
    long totalContentBytesByEventId(UUID eventId);

}
