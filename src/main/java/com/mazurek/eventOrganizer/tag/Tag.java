package com.mazurek.eventOrganizer.tag;

import com.mazurek.eventOrganizer.common.EntityIdentity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.Locale;
import java.util.UUID;

@Entity
@Getter
@Setter
@AllArgsConstructor
@Builder
@Table(name = "tags")
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private String name;
    public Tag() {
    }
    public Tag(String name) {
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
        Tag other = (Tag) o;
        Object identifier = EntityIdentity.identifier(this, this::getId);
        return identifier != null && identifier.equals(EntityIdentity.identifier(other, other::getId));
    }

    @Override
    public final int hashCode() {
        return Tag.class.hashCode();
    }
}
