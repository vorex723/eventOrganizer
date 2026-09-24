package com.mazurek.eventOrganizer.validators;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ java.lang.annotation.ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = MinFutureDateOffsetValidator.class)
public @interface MinFutureDateOffset {
    String message() default "Date must be later than 48 hours after the current server minute.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
    int hours() default 48;
}
