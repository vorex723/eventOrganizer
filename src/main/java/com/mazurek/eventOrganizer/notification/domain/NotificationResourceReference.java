package com.mazurek.eventOrganizer.notification.domain;

import java.util.Objects;
import java.util.UUID;

public record NotificationResourceReference(
        NotificationResourceType resourceType,
        UUID resourceId,
        NotificationResourceType parentResourceType,
        UUID parentResourceId
) {
    public NotificationResourceReference{
        Objects.requireNonNull(resourceType, "Resource type must not be null.");
        Objects.requireNonNull(resourceId, "Resource id must not be null.");

        boolean parentTypePresent = parentResourceType != null;
        boolean parentIdPresent = parentResourceId != null;

        if (parentTypePresent != parentIdPresent)
            throw new IllegalArgumentException(
                    "Parent resource type and id must either both be present or both be absent."
            );
    }

    public static NotificationResourceReference direct(
            NotificationResourceType resourceType,
            UUID resourceId
    ){
        return new NotificationResourceReference(
                resourceType,
                resourceId,
                null,
                null);
    }

    public static NotificationResourceReference nested(
            NotificationResourceType resourceType,
            UUID resourceId,
            NotificationResourceType parentResourceType,
            UUID parentResourceId
    ){
        Objects.requireNonNull(parentResourceType, "Parent resource type must not be null.");
        Objects.requireNonNull(parentResourceId, "Parent resource id must not be null.");

        return new NotificationResourceReference(
                resourceType,
                resourceId,
                parentResourceType,
                parentResourceId
        );
    }
}
