package com.mazurek.eventOrganizer.validators;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class ValidPasswordValidator implements ConstraintValidator<ValidPassword, String> {
    private static final Pattern PASSWORD = Pattern.compile(ValidationConstraints.PASSWORD_PATTERN);

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value == null || PASSWORD.matcher(value).matches();
    }
}
