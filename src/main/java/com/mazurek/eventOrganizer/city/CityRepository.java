package com.mazurek.eventOrganizer.city;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface CityRepository extends JpaRepository<City, UUID> {
    Optional<City> findByExternalId(String externalId);

    @Modifying
    @Query(value = """
            INSERT INTO cities (id, external_id, name, country_code, admin_area, latitude, longitude, time_zone_id)
            VALUES (:id, :externalId, :name, :countryCode, :adminArea, :latitude, :longitude, :timeZoneId)
            ON CONFLICT (external_id) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("id") UUID id, @Param("externalId") String externalId,
                       @Param("name") String name, @Param("countryCode") String countryCode,
                       @Param("adminArea") String adminArea, @Param("latitude") double latitude,
                       @Param("longitude") double longitude, @Param("timeZoneId") String timeZoneId);

    @Query("SELECT COUNT(e) FROM Event e WHERE e.city.id = :cityId")
    long countEventsByCityId(@Param("cityId") UUID cityId);
}
