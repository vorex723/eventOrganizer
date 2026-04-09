package com.mazurek.eventOrganizer.validators;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
public class MinFutureDateOffsetValidator implements ConstraintValidator<MinFutureDateOffset, Instant> {
    private int hours;
    private Clock clock = Clock.systemUTC();

    public MinFutureDateOffsetValidator() {
    }

    @Autowired
    public MinFutureDateOffsetValidator(Clock clock) {
        this.clock = clock;
    }

    public void initialize(MinFutureDateOffset constraintAnnotation) {
        this.hours = constraintAnnotation.hours();
    }

    @Override
    public boolean isValid(Instant zonedDateTime, ConstraintValidatorContext constraintValidatorContext) {
        if(zonedDateTime == null) return true;
        return zonedDateTime.isAfter(clock.instant().plus(hours, ChronoUnit.HOURS));
    }
}
