package com.mazurek.eventOrganizer.tag;

import com.mazurek.eventOrganizer.exception.tag.TagNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class TagService {

    private final TagRepository tagRepository;


    @Transactional
    public TagDto getTagByName(String tagName){
        Tag tag = tagRepository.findByIgnoreCaseName(tagName).orElseThrow(() -> new TagNotFoundException("There is no tag with that name."));
        return new TagDto(tag);
    }

}
