package com.mazurek.eventOrganizer.validators;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

public class MinFutureDateOffsetValidator implements ConstraintValidator<MinFutureDateOffset, Instant> {
    private int hours;

    public void initialize(MinFutureDateOffset constraintAnnotation) {
        this.hours = constraintAnnotation.hours();
    }

    @Override
    public boolean isValid(Instant zonedDateTime, ConstraintValidatorContext constraintValidatorContext) {
        if(zonedDateTime == null) return true;
        return zonedDateTime.isAfter(Instant.now().plus(hours, ChronoUnit.HOURS));
    }
}
