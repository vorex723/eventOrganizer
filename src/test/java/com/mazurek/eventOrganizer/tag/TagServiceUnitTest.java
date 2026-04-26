package com.mazurek.eventOrganizer.tag;

import com.mazurek.eventOrganizer.exception.tag.TagNotFoundException;
import com.mazurek.eventOrganizer.testData.TestConstants;
import com.mazurek.eventOrganizer.testData.builders.TagTestBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TagService unit tests:")
class TagServiceUnitTest {

    @Mock
    private TagRepository tagRepository;

    @InjectMocks
    private TagService tagService;

    @Nested
    @DisplayName("Get tag by name tests:")
    class GetTagByNameTests {

        @Test
        @DisplayName("When getting tag by name should return dto with correct data")
        void whenTagExistsShouldReturnDtoWithCorrectData() {
            Tag storedTag = TagTestBuilder.firstTag().build();
            when(tagRepository.findByIgnoreCaseName(TestConstants.TagConstants.FIRST_TAG_NAME))
                    .thenReturn(Optional.of(storedTag));

            TagDto result = tagService.getTagByName(TestConstants.TagConstants.FIRST_TAG_NAME);

            assertThat(result.getId()).isEqualTo(TestConstants.TagConstants.FIRST_TAG_ID);
            assertThat(result.getName()).isEqualTo(TestConstants.TagConstants.FIRST_TAG_NAME);
        }

        @Test
        @DisplayName("When getting tag by name should throw TagNotFoundException if tag does not exist")
        void whenTagDoesNotExistShouldThrowTagNotFoundException() {
            when(tagRepository.findByIgnoreCaseName(TestConstants.TagConstants.FOURTH_TAG_NAME))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> tagService.getTagByName(TestConstants.TagConstants.FOURTH_TAG_NAME))
                    .isInstanceOf(TagNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Get tags by names tests:")
    class GetTagsByNamesTests {

        @Test
        @DisplayName("When getting tags by names should return existing tags without creating new ones")
        void whenTagsExistShouldReturnExistingTagsWithoutCreatingNewOnes() {
            Tag firstTag = TagTestBuilder.firstTag().build();
            Tag secondTag = TagTestBuilder.secondTag().build();

            when(tagRepository.findByIgnoreCaseName(TestConstants.TagConstants.FIRST_TAG_NAME))
                    .thenReturn(Optional.of(firstTag));
            when(tagRepository.findByIgnoreCaseName(TestConstants.TagConstants.SECOND_TAG_NAME))
                    .thenReturn(Optional.of(secondTag));

            Set<Tag> result = tagService.getTagsByNames(TestConstants.TagConstants.DEFAULT_EVENT_TAGS);

            assertThat(result).hasSize(2);
            verify(tagRepository, never()).save(any(Tag.class));
        }

        @Test
        @DisplayName("When getting tags by names should create missing tags in lowercase and return them")
        void whenTagDoesNotExistShouldCreateItInLowercaseAndReturnIt() {
            Tag savedTag = TagTestBuilder.thirdTag().name(TestConstants.TagConstants.FOURTH_TAG_NAME).build();

            when(tagRepository.findByIgnoreCaseName(TestConstants.TagConstants.FOURTH_TAG_NAME.toUpperCase()))
                    .thenReturn(Optional.empty());
            when(tagRepository.save(any(Tag.class))).thenReturn(savedTag);

            Set<Tag> result = tagService.getTagsByNames(Set.of(TestConstants.TagConstants.FOURTH_TAG_NAME.toUpperCase()));

            assertThat(result).hasSize(1);
            assertThat(result.iterator().next().getName()).isEqualTo(TestConstants.TagConstants.FOURTH_TAG_NAME);
            verify(tagRepository, times(1)).save(any(Tag.class));
        }
    }
}
