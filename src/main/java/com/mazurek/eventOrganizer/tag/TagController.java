package com.mazurek.eventOrganizer.tag;

import com.mazurek.eventOrganizer.exception.tag.TagNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Collections;

@RequiredArgsConstructor
@Controller
@RequestMapping("/api/v1/tags")
public class TagController {

    private final TagService tagService;

    @GetMapping("/{tagName}")
    public ResponseEntity<?> getTag(@PathVariable("tagName") String tagName){
        try{
            return ResponseEntity.ok(tagService.getTagByName(tagName));
        } catch (TagNotFoundException exception) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Collections.singletonMap("Message", exception.getMessage()));
        }
        catch (RuntimeException exception){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
