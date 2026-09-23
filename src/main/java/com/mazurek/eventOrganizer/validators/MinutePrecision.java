package com.mazurek.eventOrganizer.validators;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(java.lang.annotation.ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = MinutePrecisionValidator.class)
public @interface MinutePrecision {
    String message() default "Date and time must be specified to minute precision.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
