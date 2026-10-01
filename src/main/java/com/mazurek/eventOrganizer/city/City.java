package com.mazurek.eventOrganizer.city;

import com.mazurek.eventOrganizer.common.EntityIdentity;
import jakarta.persistence.*;
import lombok.*;

import java.util.Locale;
import java.util.UUID;


@Getter
@Setter
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "cities",
        uniqueConstraints = @UniqueConstraint(columnNames = "name")
)
public class City {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private String name;
    public City() {
    }

    public City(String name) {
        this.name = name.toLowerCase(Locale.ROOT).trim();
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || EntityIdentity.persistentClass(this) != EntityIdentity.persistentClass(o)) {
            return false;
        }
        City other = (City) o;
        Object identifier = EntityIdentity.identifier(this, this::getId);
        return identifier != null && identifier.equals(EntityIdentity.identifier(other, other::getId));
    }

    @Override
    public final int hashCode() {
        return City.class.hashCode();
    }
}
