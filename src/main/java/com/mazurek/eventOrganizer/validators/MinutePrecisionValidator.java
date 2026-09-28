package com.mazurek.eventOrganizer.validators;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class MinutePrecisionValidator implements ConstraintValidator<MinutePrecision, Instant> {

    @Override
    public boolean isValid(Instant value, ConstraintValidatorContext context) {
        return value == null || value.equals(value.truncatedTo(ChronoUnit.MINUTES));
    }
}
