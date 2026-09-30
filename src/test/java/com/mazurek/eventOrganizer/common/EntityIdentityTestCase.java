package com.mazurek.eventOrganizer.common;

import com.mazurek.eventOrganizer.city.City;
import com.mazurek.eventOrganizer.event.Event;
import com.mazurek.eventOrganizer.file.File;
import com.mazurek.eventOrganizer.tag.Tag;
import com.mazurek.eventOrganizer.thread.Thread;
import com.mazurek.eventOrganizer.threadReply.ThreadReply;
import com.mazurek.eventOrganizer.user.User;

import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

record EntityIdentityTestCase(Class<?> entityClass, Supplier<Object> factory,
                              BiConsumer<Object, UUID> idSetter, Function<Object, UUID> idGetter) {

    static Stream<EntityIdentityTestCase> cases() {
        return Stream.of(
                of(User.class, User::new, User::setId, User::getId),
                of(Event.class, Event::new, Event::setId, Event::getId),
                of(City.class, City::new, City::setId, City::getId),
                of(Tag.class, Tag::new, Tag::setId, Tag::getId),
                of(File.class, File::new, File::setId, File::getId),
                of(Thread.class, Thread::new, Thread::setId, Thread::getId),
                of(ThreadReply.class, ThreadReply::new, ThreadReply::setId, ThreadReply::getId)
        );
    }

    private static <T> EntityIdentityTestCase of(Class<T> type, Supplier<T> factory,
                                                BiConsumer<T, UUID> setter, Function<T, UUID> getter) {
        return new EntityIdentityTestCase(type, factory::get,
                (entity, id) -> setter.accept(type.cast(entity), id),
                entity -> getter.apply(type.cast(entity)));
    }

    Object newEntity(UUID id) {
        Object entity = factory.get();
        idSetter.accept(entity, id);
        return entity;
    }

    @Override
    public String toString() {
        return entityClass.getSimpleName();
    }
}
