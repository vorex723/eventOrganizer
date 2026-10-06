package com.mazurek.eventOrganizer.common;

import com.mazurek.eventOrganizer.city.City;
import com.mazurek.eventOrganizer.event.Event;
import com.mazurek.eventOrganizer.file.File;
import com.mazurek.eventOrganizer.tag.Tag;
import com.mazurek.eventOrganizer.thread.Thread;
import com.mazurek.eventOrganizer.threadReply.ThreadReply;
import com.mazurek.eventOrganizer.user.User;
import com.mazurek.eventOrganizer.testData.builders.CityTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.EventTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.FileTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.TagTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.ThreadTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.ThreadReplyTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.UserTestBuilder;

import java.util.UUID;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

record EntityIdentityTestCase(Class<?> entityClass, Supplier<Object> factory,
                              BiConsumer<Object, UUID> idSetter, Function<Object, UUID> idGetter) {

    static Stream<EntityIdentityTestCase> cases() {
        return Stream.of(
                of(User.class, () -> UserTestBuilder.firstUser().id(null).homeCity(null).roles(Set.of()).build(), User::setId, User::getId),
                of(Event.class, () -> EventTestBuilder.firstEvent().id(null).city(null).owner(null).build(), Event::setId, Event::getId),
                of(City.class, () -> CityTestBuilder.warsaw().id(null).build(), City::setId, City::getId),
                of(Tag.class, () -> TagTestBuilder.firstTag().id(null).build(), Tag::setId, Tag::getId),
                of(File.class, () -> FileTestBuilder.jpgFile().id(null).event(null).owner(null).build(), File::setId, File::getId),
                of(Thread.class, () -> ThreadTestBuilder.firstThread().id(null).event(null).owner(null).build(), Thread::setId, Thread::getId),
                of(ThreadReply.class, () -> ThreadReplyTestBuilder.firstReply().id(null).thread(null).replier(null).build(), ThreadReply::setId, ThreadReply::getId)
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
