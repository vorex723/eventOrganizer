package com.mazurek.eventOrganizer.validators;

import com.mazurek.eventOrganizer.config.properties.CommunityProperties;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ValidEventCapacityValidator implements ConstraintValidator<ValidEventCapacity, Integer> {

    private final CommunityProperties communityProperties;

    @Override
    public boolean isValid(Integer value, ConstraintValidatorContext context) {
        return value == null || (value >= 1 && value <= communityProperties.getMaxAttendees());
    }
}
