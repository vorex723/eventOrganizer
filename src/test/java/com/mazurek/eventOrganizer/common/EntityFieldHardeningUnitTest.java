package com.mazurek.eventOrganizer.common;

import com.mazurek.eventOrganizer.event.Event;
import com.mazurek.eventOrganizer.file.File;
import com.mazurek.eventOrganizer.thread.Thread;
import com.mazurek.eventOrganizer.threadReply.ThreadReply;
import com.mazurek.eventOrganizer.testData.builders.EventTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.FileTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.ThreadReplyTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.ThreadTestBuilder;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class EntityFieldHardeningUnitTest {
    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void initializeValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        factory.close();
    }

    static Stream<Arguments> requiredFields() {
        return Stream.of(
                required(EventTestBuilder.firstEvent()::build, "name"),
                required(EventTestBuilder.firstEvent()::build, "shortDescription"),
                required(EventTestBuilder.firstEvent()::build, "longDescription"),
                required(EventTestBuilder.firstEvent()::build, "exactAddress"),
                required(EventTestBuilder.firstEvent()::build, "createDate"),
                required(EventTestBuilder.firstEvent()::build, "lastUpdate"),
                required(EventTestBuilder.firstEvent()::build, "eventStartDate"),
                required(EventTestBuilder.firstEvent()::build, "city"),
                required(ThreadTestBuilder.firstThread()::build, "event"),
                required(ThreadTestBuilder.firstThread()::build, "name"),
                required(ThreadTestBuilder.firstThread()::build, "content"),
                required(ThreadTestBuilder.firstThread()::build, "createDate"),
                required(ThreadTestBuilder.firstThread()::build, "lastUpdate"),
                required(ThreadTestBuilder.firstThread()::build, "lastActivity"),
                required(ThreadTestBuilder.firstThread()::build, "editCount"),
                required(ThreadTestBuilder.firstThread()::build, "replyCount"),
                required(ThreadReplyTestBuilder.firstReply()::build, "thread"),
                required(ThreadReplyTestBuilder.firstReply()::build, "content"),
                required(ThreadReplyTestBuilder.firstReply()::build, "replyDate"),
                required(ThreadReplyTestBuilder.firstReply()::build, "lastUpdate"),
                required(ThreadReplyTestBuilder.firstReply()::build, "editCount"),
                required(FileTestBuilder.jpgFile()::build, "event"),
                required(FileTestBuilder.jpgFile()::build, "userFileName"),
                required(FileTestBuilder.jpgFile()::build, "originalFileName"),
                required(FileTestBuilder.jpgFile()::build, "contentType"),
                required(FileTestBuilder.jpgFile()::build, "content"),
                required(FileTestBuilder.jpgFile()::build, "uploadDateTime")
        );
    }

    private static Arguments required(Supplier<?> supplier, String field) {
        return Arguments.of(supplier, field);
    }

    @ParameterizedTest(name = "required field {1}")
    @MethodSource("requiredFields")
    void rejectsMissingRequiredValues(Supplier<?> supplier, String field) throws Exception {
        Object entity = supplier.get();
        setField(entity, field, null);
        assertThat(validator.validate(entity)).anyMatch(violation -> violation.getPropertyPath().toString().equals(field));
    }

    static Stream<Arguments> invalidValues() {
        return Stream.of(
                invalid(EventTestBuilder.firstEvent()::build, "name", " \t\n"),
                invalid(EventTestBuilder.firstEvent()::build, "name", "x".repeat(256)),
                invalid(EventTestBuilder.firstEvent()::build, "attendeeCount", -1),
                invalid(EventTestBuilder.firstEvent()::build, "maxAttendees", 0),
                invalid(ThreadTestBuilder.firstThread()::build, "editCount", -1),
                invalid(ThreadTestBuilder.firstThread()::build, "replyCount", -1),
                invalid(ThreadTestBuilder.firstThread()::build, "version", -1L),
                invalid(ThreadTestBuilder.firstThread()::build, "content", "x".repeat(1001)),
                invalid(ThreadReplyTestBuilder.firstReply()::build, "editCount", -1),
                invalid(ThreadReplyTestBuilder.firstReply()::build, "version", -1L),
                invalid(ThreadReplyTestBuilder.firstReply()::build, "content", " \t"),
                invalid(FileTestBuilder.jpgFile()::build, "contentType", "")
        );
    }

    private static Arguments invalid(Supplier<?> supplier, String field, Object value) {
        return Arguments.of(supplier, field, value);
    }

    @ParameterizedTest(name = "invalid value of {1}")
    @MethodSource("invalidValues")
    void rejectsBlankOversizedAndNegativeValues(Supplier<?> supplier, String field, Object value) throws Exception {
        Object entity = supplier.get();
        setField(entity, field, value);
        assertThat(validator.validate(entity)).anyMatch(violation -> violation.getPropertyPath().toString().equals(field));
    }

    @Test
    void allowsAnonymousAuthorsAndUnlimitedEventCapacity() {
        Event event = EventTestBuilder.firstEvent().owner(null).maxAttendees(null).build();
        Thread thread = ThreadTestBuilder.firstThread().owner(null).build();
        ThreadReply reply = ThreadReplyTestBuilder.firstReply().replier(null).build();
        File file = FileTestBuilder.jpgFile().owner(null).build();
        assertThat(validator.validate(event)).isEmpty();
        assertThat(validator.validate(thread)).isEmpty();
        assertThat(validator.validate(reply)).isEmpty();
        assertThat(validator.validate(file)).isEmpty();
    }

    private static void setField(Object entity, String fieldName, Object value) throws Exception {
        var field = entity.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(entity, value);
    }
}
