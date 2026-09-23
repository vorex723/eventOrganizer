package com.mazurek.eventOrganizer.validators;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
@RequiredArgsConstructor
public class MinFutureDateOffsetValidator implements ConstraintValidator<MinFutureDateOffset, Instant> {
    private int hours;
    private final Clock clock;

    public void initialize(MinFutureDateOffset constraintAnnotation) {
        this.hours = constraintAnnotation.hours();
    }

    @Override
    public boolean isValid(Instant zonedDateTime, ConstraintValidatorContext constraintValidatorContext) {
        if(zonedDateTime == null) return true;
        Instant earliestAllowed = clock.instant()
                .truncatedTo(ChronoUnit.MINUTES)
                .plus(hours, ChronoUnit.HOURS);
        return !zonedDateTime.isBefore(earliestAllowed);
    }
}
