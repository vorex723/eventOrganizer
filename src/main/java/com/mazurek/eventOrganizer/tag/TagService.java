package com.mazurek.eventOrganizer.tag;

import com.mazurek.eventOrganizer.exception.tag.TagNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class TagService {

    private final TagRepository tagRepository;


    @Transactional
    public TagDto getTagByName(String tagName){
        Tag tag = tagRepository.findByIgnoreCaseName(tagName).orElseThrow(TagNotFoundException::new);
        return new TagDto(tag);
    }

    @Transactional
    public Set<Tag> getTagsByNames(Set<String> tagNames){
        return tagNames.stream()
                .map(tagName -> tagRepository.findByIgnoreCaseName(tagName).orElseGet(() -> tagRepository.save(new Tag(tagName.toLowerCase(Locale.ROOT)))))
                .collect(Collectors.toSet());
    }
}
