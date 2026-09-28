package com.mazurek.eventOrganizer.file;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    @Modifying(flushAutomatically = true)
    @Query("""
            update File file
            set file.ownerNameAtCreation = 'Deleted user'
            where file.owner.id = :userId
            """)
    int anonymizeOwnerSnapshotsByUserId(@Param("userId") UUID userId);

    long countByEventId(UUID eventId);

    @Query("""
            SELECT f.id AS id, f.userFileName AS userFileName, f.originalFileName AS originalFileName,
                   f.contentType AS contentType, f.uploadDateTime AS uploadDateTime,
                   o.id AS ownerId, o.firstName AS ownerFirstName, o.lastName AS ownerLastName,
                   c.name AS ownerHomeCity
            FROM File f
            LEFT JOIN f.owner o
            LEFT JOIN o.homeCity c
            WHERE f.id = :fileId AND f.event.id = :eventId
            """)
    Optional<FileOverviewProjection> findOverviewByIdAndEventId(UUID fileId, UUID eventId);

    @Query("""
            SELECT f.id AS id, f.userFileName AS userFileName, f.originalFileName AS originalFileName,
                   f.contentType AS contentType, f.uploadDateTime AS uploadDateTime,
                   o.id AS ownerId, o.firstName AS ownerFirstName, o.lastName AS ownerLastName,
                   c.name AS ownerHomeCity
            FROM File f
            LEFT JOIN f.owner o
            LEFT JOIN o.homeCity c
            WHERE f.event.id = :eventId
            """)
    Page<FileOverviewProjection> findOverviewsByEventId(UUID eventId, Pageable pageable);

    @Query("""
            SELECT f.contentType AS contentType, f.content AS content
            FROM File f
            WHERE f.id = :fileId AND f.event.id = :eventId
            """)
    Optional<FileContentProjection> findContentByIdAndEventId(UUID fileId, UUID eventId);

    @Query(value = "SELECT COALESCE(SUM(octet_length(content)), 0) FROM files WHERE event_id = :eventId", nativeQuery = true)
    long totalContentBytesByEventId(UUID eventId);

}
