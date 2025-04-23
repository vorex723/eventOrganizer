package com.mazurek.eventOrganizer.validators;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.ZonedDateTime;

public class MinFutureDateOffsetValidator implements ConstraintValidator<MinFutureDateOffset, ZonedDateTime> {
    private int hours;

    public void initialize(MinFutureDateOffset constraintAnnotation) {
        this.hours = constraintAnnotation.hours();
    }

    @Override
    public boolean isValid(ZonedDateTime zonedDateTime, ConstraintValidatorContext constraintValidatorContext) {
        if(zonedDateTime == null) return true;
        return zonedDateTime.isAfter(ZonedDateTime.now().plusHours(hours));
    }
}
