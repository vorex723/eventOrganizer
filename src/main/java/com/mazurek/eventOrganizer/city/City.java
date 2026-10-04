package com.mazurek.eventOrganizer.city;

import com.mazurek.eventOrganizer.common.EntityIdentity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.ZoneId;
import java.util.Locale;
import java.util.UUID;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "cities", uniqueConstraints = @UniqueConstraint(name = "uk_city_external_id", columnNames = "external_id"))
public class City {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Setter
    private UUID id;
    @Size(max = 255)
    @NotBlank
    @Column(name = "external_id", nullable = false, length = 255)
    private String externalId;
    @Size(max = 255)
    @NotBlank
    @Column(nullable = false, length = 255)
    private String name;
    @Size(max = 2)
    @NotBlank
    @Column(name = "country_code", nullable = false, length = 2)
    private String countryCode;
    @Size(max = 255)
    @Column(name = "admin_area", nullable = true, length = 255)
    private String adminArea;
    @Column(nullable = false)
    private double latitude;
    @Column(nullable = false)
    private double longitude;
    @Size(max = 255)
    @NotBlank
    @Column(name = "time_zone_id", nullable = false, length = 255)
    private String timeZoneId;

    public City(String externalId, String name, String countryCode, String adminArea,
                double latitude, double longitude, String timeZoneId) {
        this.externalId = requireText(externalId, "externalId");
        this.name = requireText(name, "name");
        if (countryCode == null || !countryCode.strip().matches("[a-zA-Z]{2}")) {
            throw new IllegalArgumentException("countryCode must contain two letters");
        }
        this.countryCode = countryCode.strip().toUpperCase(Locale.ROOT);
        if (adminArea != null && adminArea.strip().length() > 255) {
            throw new IllegalArgumentException("adminArea cannot exceed 255 characters");
        }
        this.adminArea = adminArea == null || adminArea.isBlank() ? null : adminArea.strip();
        if (!Double.isFinite(latitude) || latitude < -90 || latitude > 90
                || !Double.isFinite(longitude) || longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("City coordinates are invalid");
        }
        this.latitude = latitude;
        this.longitude = longitude;
        this.timeZoneId = requireText(timeZoneId, "timeZoneId");
        if (!ZoneId.getAvailableZoneIds().contains(this.timeZoneId)) {
            throw new IllegalArgumentException("timeZoneId must be an IANA timezone identifier");
        }
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank() || value.strip().length() > 255) {
            throw new IllegalArgumentException(field + " must contain 1 to 255 characters");
        }
        return value.strip();
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || EntityIdentity.persistentClass(this) != EntityIdentity.persistentClass(o)) return false;
        City other = (City) o;
        Object identifier = EntityIdentity.identifier(this, this::getId);
        return identifier != null && identifier.equals(EntityIdentity.identifier(other, other::getId));
    }

    @Override
    public final int hashCode() {
        return City.class.hashCode();
    }
}
