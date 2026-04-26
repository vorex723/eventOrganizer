package com.mazurek.eventOrganizer.tag;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/tags")
public class TagController {

    private final TagService tagService;

    @GetMapping("/{tagName}")
    public ResponseEntity<TagDto> getTag(@PathVariable("tagName") String tagName){
            return ResponseEntity.ok(tagService.getTagByName(tagName));
    }
}
