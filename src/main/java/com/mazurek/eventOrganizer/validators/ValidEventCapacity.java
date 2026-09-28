package com.mazurek.eventOrganizer.validators;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = ValidEventCapacityValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidEventCapacity {

    String message() default "Maximum attendee capacity must be between 1 and the configured maximum.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
