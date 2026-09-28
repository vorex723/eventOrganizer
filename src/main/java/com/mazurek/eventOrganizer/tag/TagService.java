package com.mazurek.eventOrganizer.tag;

import com.mazurek.eventOrganizer.exception.tag.TagNotFoundException;
import com.mazurek.eventOrganizer.validators.LookupNameValidator;
import com.mazurek.eventOrganizer.validators.ValidationConstraints;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class TagService {

    private final TagRepository tagRepository;


    @Transactional
    public TagDto getTagByName(String tagName){
        Tag tag = getTagByNameOrThrow(tagName);
        TagDto dto = new TagDto(tag);
        dto.setEventCount(tagRepository.countEventsByTagId(tag.getId()));
        return dto;
    }

    @Transactional(readOnly = true)
    public Tag getTagByNameOrThrow(String tagName) {
        LookupNameValidator.requireValid(
                tagName,
                ValidationConstraints.TAG_MIN_LENGTH,
                ValidationConstraints.TAG_MAX_LENGTH,
                "Tag name"
        );
        return tagRepository.findByIgnoreCaseName(tagName.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(TagNotFoundException::new);
    }

    @Transactional
    public Set<Tag> getTagsByNames(Set<String> tagNames){
        return tagNames.stream()
                .map(this::normalizeAndValidateTagName)
                .distinct()
                .map(this::getOrCreateTag)
                .collect(Collectors.toSet());
    }

    private String normalizeAndValidateTagName(String tagName) {
        LookupNameValidator.requireValid(
                tagName,
                ValidationConstraints.TAG_MIN_LENGTH,
                ValidationConstraints.TAG_MAX_LENGTH,
                "Tag name"
        );
        return tagName.trim().toLowerCase(Locale.ROOT);
    }

    private Tag getOrCreateTag(String normalizedName) {
        var existing = tagRepository.findByIgnoreCaseName(normalizedName);
        if (existing.isPresent()) {
            return existing.get();
        }
        tagRepository.insertIfAbsent(UUID.randomUUID(), normalizedName);
        return tagRepository.findByIgnoreCaseName(normalizedName).orElseThrow();
    }
}
