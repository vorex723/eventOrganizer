package com.mazurek.eventOrganizer.community;

import com.mazurek.eventOrganizer.DeletionService;
import com.mazurek.eventOrganizer.auth.AuthenticationService;
import com.mazurek.eventOrganizer.city.City;
import com.mazurek.eventOrganizer.city.CityRepository;
import com.mazurek.eventOrganizer.city.CityService;
import com.mazurek.eventOrganizer.event.Event;
import com.mazurek.eventOrganizer.event.EventRepository;
import com.mazurek.eventOrganizer.event.EventService;
import com.mazurek.eventOrganizer.exception.event.EventCapacityReachedException;
import com.mazurek.eventOrganizer.file.File;
import com.mazurek.eventOrganizer.file.FileRepository;
import com.mazurek.eventOrganizer.file.FileService;
import com.mazurek.eventOrganizer.file.FileUploadDto;
import com.mazurek.eventOrganizer.jwt.JwtUserDetails;
import com.mazurek.eventOrganizer.tag.Tag;
import com.mazurek.eventOrganizer.tag.TagRepository;
import com.mazurek.eventOrganizer.tag.TagService;
import com.mazurek.eventOrganizer.testData.AuthHelper;
import com.mazurek.eventOrganizer.testData.TestDataInitializer;
import com.mazurek.eventOrganizer.testData.builders.dto.FileUploadDtoTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.dto.MultipartFileTestBuilder;
import com.mazurek.eventOrganizer.user.User;
import com.mazurek.eventOrganizer.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static com.mazurek.eventOrganizer.testData.TestConstants.EventConstants;
import static com.mazurek.eventOrganizer.testData.TestConstants.FileConstants;
import static com.mazurek.eventOrganizer.testData.TestConstants.UserConstants;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class CommunityConcurrencyIntegrationTest {

    @Autowired private DeletionService deletionService;
    @Autowired private AuthHelper authHelper;
    @Autowired private CityService cityService;
    @Autowired private CityRepository cityRepository;
    @Autowired private TagService tagService;
    @Autowired private TagRepository tagRepository;
    @Autowired private EventService eventService;
    @Autowired private EventRepository eventRepository;
    @Autowired private FileService fileService;
    @Autowired private FileRepository fileRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private TestDataInitializer testDataInitializer;

    @BeforeEach
    void setUp() {
        deletionService.deleteAllSafe();
        authHelper.setupRolesAndUsers();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void concurrentCityCreationReturnsOneNormalizedCity() throws Exception {
        List<City> cities = runConcurrently(
                () -> cityService.getCityByNameOrCreate("  Concurrent City  "),
                () -> cityService.getCityByNameOrCreate("concurrent city")
        );

        assertThat(cities).extracting(City::getId).containsOnly(cities.getFirst().getId());
        assertThat(cityRepository.findAll().stream()
                .filter(city -> city.getName().equals("concurrent city")))
                .hasSize(1);
    }

    @Test
    void concurrentTagCreationReturnsOneNormalizedTag() throws Exception {
        List<Tag> tags = runConcurrently(
                () -> tagService.getTagsByNames(java.util.Set.of(" Concurrent Tag ")).iterator().next(),
                () -> tagService.getTagsByNames(java.util.Set.of("concurrent tag")).iterator().next()
        );

        assertThat(tags).extracting(Tag::getId).containsOnly(tags.getFirst().getId());
        assertThat(tagRepository.findAll().stream()
                .filter(tag -> tag.getName().equals("concurrent tag")))
                .hasSize(1);
    }

    @Test
    void concurrentFinalSeatRequestsAllowExactlyOneAttendee() throws Exception {
        UUID eventId = testDataInitializer.setupFirstEvent();
        Event event = eventRepository.findById(eventId).orElseThrow();
        event.setMaxAttendees(1);
        eventRepository.saveAndFlush(event);

        User firstCandidate = userRepository.findByIgnoreCaseEmail(UserConstants.SECOND_USER_EMAIL).orElseThrow();
        User secondCandidate = userRepository.save(User.builder()
                .firstName("Concurrent")
                .lastName("Candidate")
                .email("concurrent.candidate@example.com")
                .password("unused")
                .homeCity(firstCandidate.getHomeCity())
                .timeZone(firstCandidate.getTimeZone())
                .createdAt(Instant.now())
                .lastCredentialsChangeTime(Instant.now())
                .activated(true)
                .banned(false)
                .build());

        List<Boolean> outcomes = runConcurrently(
                () -> attendAs(firstCandidate, eventId),
                () -> attendAs(secondCandidate, eventId)
        );

        Event storedEvent = eventRepository.findById(eventId).orElseThrow();
        assertThat(outcomes).containsExactlyInAnyOrder(true, false);
        assertThat(storedEvent.getAttendeeCount()).isEqualTo(1);
    }

    @Test
    void concurrentFiftiethFileUploadsPersistExactlyOneFile() throws Exception {
        UUID eventId = testDataInitializer.setupFirstEvent();
        Event event = eventRepository.findById(eventId).orElseThrow();
        User owner = userRepository.findByIgnoreCaseEmail(UserConstants.FIRST_USER_EMAIL).orElseThrow();
        User attendee = userRepository.findByIgnoreCaseEmail(UserConstants.SECOND_USER_EMAIL).orElseThrow();
        testDataInitializer.addSecondUserToAttenders(eventId);

        for (int index = 0; index < 49; index++) {
            fileRepository.save(File.builder()
                    .event(event)
                    .owner(owner)
                    .userFileName("seed-" + index)
                    .originalFileName("seed-" + index + ".jpg")
                    .contentType(FileConstants.JPG_FILE_CONTENT_TYPE)
                    .content(new byte[] {1})
                    .uploadDateTime(Instant.now())
                    .build());
        }
        fileRepository.flush();

        List<Boolean> outcomes = runConcurrently(
                () -> uploadAs(owner, eventId, "owner-upload"),
                () -> uploadAs(attendee, eventId, "attendee-upload")
        );

        assertThat(outcomes).containsExactlyInAnyOrder(true, false);
        assertThat(fileRepository.countByEventId(eventId)).isEqualTo(50);
    }

    private boolean attendAs(User user, UUID eventId) {
        authenticate(user);
        try {
            eventService.addAttenderToEvent(eventId);
            return true;
        } catch (EventCapacityReachedException exception) {
            return false;
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private boolean uploadAs(User user, UUID eventId, String filename) throws Exception {
        authenticate(user);
        try {
            FileUploadDto request = FileUploadDtoTestBuilder.jpgFile()
                    .userFilename(filename)
                    .file(MultipartFileTestBuilder.jpgFile().buildMultipartFile())
                    .build();
            fileService.uploadFileToEvent(request, eventId);
            return true;
        } catch (com.mazurek.eventOrganizer.exception.file.EventFileQuotaExceededException exception) {
            return false;
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private void authenticate(User user) {
        JwtUserDetails principal = new JwtUserDetails(
                user.getId(),
                user.getEmail(),
                user.getRoles().stream().map(role -> new SimpleGrantedAuthority(role.getName())).toList()
        );
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }

    private <T> List<T> runConcurrently(ThrowingSupplier<T> first, ThrowingSupplier<T> second) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<T> firstResult = executor.submit(() -> awaitAndRun(ready, start, first));
            Future<T> secondResult = executor.submit(() -> awaitAndRun(ready, start, second));
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            return List.of(firstResult.get(10, TimeUnit.SECONDS), secondResult.get(10, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
        }
    }

    private <T> T awaitAndRun(CountDownLatch ready, CountDownLatch start, ThrowingSupplier<T> action) throws Exception {
        ready.countDown();
        if (!start.await(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Concurrent test did not receive its start signal.");
        }
        return action.get();
    }

    @FunctionalInterface
    private interface ThrowingSupplier<T> {
        T get() throws Exception;
    }
}
