package com.mazurek.eventOrganizer.city;

import com.mazurek.eventOrganizer.event.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface CityRepository extends JpaRepository<City, UUID> {
    Optional<City> findByIgnoreCaseName(String name);

    @Query(value = "SELECT * FROM cities WHERE lower(btrim(name)) = :name", nativeQuery = true)
    Optional<City> findByNormalizedName(@Param("name") String name);

    @Modifying
    @Query(value = "INSERT INTO cities (id, name) VALUES (:id, :name) ON CONFLICT ((lower(btrim(name)))) DO NOTHING", nativeQuery = true)
    int insertIfAbsent(@Param("id") UUID id, @Param("name") String name);

    @Query("SELECT COUNT(e) FROM Event e WHERE e.city.id = :cityId")
    long countEventsByCityId(@Param("cityId") UUID cityId);
}
