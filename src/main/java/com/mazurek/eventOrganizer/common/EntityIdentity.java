package com.mazurek.eventOrganizer.common;

import org.hibernate.proxy.HibernateProxy;

import java.util.function.Supplier;

/**
 * Reads identity without initializing Hibernate proxies.
 * Intended for entities without polymorphic inheritance.
 */
public final class EntityIdentity {

    private EntityIdentity() {
    }

    public static Class<?> persistentClass(Object entity) {
        if (entity instanceof HibernateProxy proxy) {
            return proxy.getHibernateLazyInitializer().getPersistentClass();
        }
        return entity.getClass();
    }

    public static Object identifier(Object entity, Supplier<?> identifierGetter) {
        if (entity instanceof HibernateProxy proxy) {
            return proxy.getHibernateLazyInitializer().getInternalIdentifier();
        }
        return identifierGetter.get();
    }
}
