package com.mazurek.eventOrganizer.community;

import com.mazurek.eventOrganizer.DeletionService;
import com.mazurek.eventOrganizer.city.City;
import com.mazurek.eventOrganizer.city.CityRepository;
import com.mazurek.eventOrganizer.city.CityServiceImpl;
import com.mazurek.eventOrganizer.event.Event;
import com.mazurek.eventOrganizer.event.EventRepository;
import com.mazurek.eventOrganizer.event.EventService;
import com.mazurek.eventOrganizer.event.dto.EventCreateDto;
import com.mazurek.eventOrganizer.exception.event.EventCapacityReachedException;
import com.mazurek.eventOrganizer.exception.event.EventCapacityTooSmallException;
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
import com.mazurek.eventOrganizer.testData.builders.FileTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.JwtUserDetailsTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.UserTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.dto.EventCreateDtoTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.dto.FileUploadDtoTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.dto.MultipartFileTestBuilder;
import com.mazurek.eventOrganizer.user.User;
import com.mazurek.eventOrganizer.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import com.mazurek.eventOrganizer.testSupport.concurrency.TestWorkers;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static com.mazurek.eventOrganizer.testData.TestConstants.FileConstants;
import static com.mazurek.eventOrganizer.testData.TestConstants.TagConstants;
import static com.mazurek.eventOrganizer.testData.TestConstants.TimeConstants;
import static com.mazurek.eventOrganizer.testData.TestConstants.UserConstants;
import static com.mazurek.eventOrganizer.testData.TestFailureHelper.requirePresent;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Community concurrency integration tests:")
class CommunityConcurrencyIntegrationTest {

    @Autowired private DeletionService deletionService;
    @Autowired private AuthHelper authHelper;
    @Autowired private CityServiceImpl cityService;
    @Autowired private CityRepository cityRepository;
    @Autowired private TagService tagService;
    @Autowired private TagRepository tagRepository;
    @Autowired private EventService eventService;
    @Autowired private EventRepository eventRepository;
    @Autowired private FileService fileService;
    @Autowired private FileRepository fileRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private TestDataInitializer testDataInitializer;
    @Autowired private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        deletionService.deleteAllSafe();
        authHelper.setupRolesAndUsers();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        deletionService.deleteAllSafe();
    }

    @Test
    void whenResolvingCityConcurrentlyShouldReturnOneCityForExternalId() throws Exception {
        List<City> cities = runConcurrently(
                () -> cityService.resolve("  test:concurrent city  "),
                () -> cityService.resolve("test:concurrent city")
        );

        assertThat(cities).hasSize(2).doesNotContainNull();
        assertThat(cities.getFirst().getId()).isNotNull();
        assertThat(cities).extracting(City::getId).containsExactly(cities.getFirst().getId(), cities.getFirst().getId());
        assertThat(cities).extracting(City::getExternalId).containsOnly("test:concurrent city");
        assertThat(requirePresent(cityRepository.findByExternalId("test:concurrent city"),
                "Expected one persisted city after concurrent resolve").getId()).isEqualTo(cities.getFirst().getId());
        assertThat(cityRepository.findAll().stream()
                .filter(city -> city.getName().equals("concurrent city")))
                .hasSize(1);
    }

    @Test
    void whenCreatingTagConcurrentlyShouldReturnOneNormalizedTag() throws Exception {
        List<Tag> tags = runConcurrently(
                () -> tagService.getTagsByNames(java.util.Set.of(" Concurrent Tag ")).iterator().next(),
                () -> tagService.getTagsByNames(java.util.Set.of("concurrent tag")).iterator().next()
        );

        assertThat(tags).hasSize(2).doesNotContainNull();
        assertThat(tags.getFirst().getId()).isNotNull();
        assertThat(tags).extracting(Tag::getId).containsExactly(tags.getFirst().getId(), tags.getFirst().getId());
        assertThat(tags).extracting(Tag::getName).containsOnly("concurrent tag");
        assertThat(requirePresent(tagRepository.findByIgnoreCaseName("concurrent tag"),
                "Expected one persisted normalized tag after concurrent creation").getId()).isEqualTo(tags.getFirst().getId());
        assertThat(tagRepository.findAll().stream()
                .filter(tag -> tag.getName().equals("concurrent tag")))
                .hasSize(1);
    }

    @Test
    void whenFinalSeatIsRequestedConcurrentlyShouldAllowExactlyOneAttendee() throws Exception {
        UUID eventId = testDataInitializer.setupFirstEvent();
        Event event = requirePresent(eventRepository.findById(eventId), "Expected event record in whenFinalSeatIsRequestedConcurrentlyShouldAllowExactlyOneAttendee");
        event.setMaxAttendees(1);
        eventRepository.saveAndFlush(event);

        User firstCandidate = requirePresent(userRepository.findByIgnoreCaseEmail(UserConstants.SECOND_USER_EMAIL), "Expected user record in whenFinalSeatIsRequestedConcurrentlyShouldAllowExactlyOneAttendee");
        User secondCandidate = createConcurrentCandidate(firstCandidate);

        List<Boolean> outcomes = runConcurrently(
                () -> attendAs(firstCandidate, eventId),
                () -> attendAs(secondCandidate, eventId)
        );

        Event storedEvent = requirePresent(eventRepository.findById(eventId), "Expected event record in whenFinalSeatIsRequestedConcurrentlyShouldAllowExactlyOneAttendee");
        assertThat(outcomes).containsExactlyInAnyOrder(true, false);
        assertThat(storedEvent.getAttendeeCount()).isEqualTo(1);
        assertThat(attendeeRows(eventId)).isEqualTo(1);
        UUID winnerId = outcomes.getFirst() ? firstCandidate.getId() : secondCandidate.getId();
        assertThat(eventRepository.findAttendeeIdsByEventId(eventId)).containsExactly(winnerId);
        assertThat(storedEvent.getMaxAttendees()).isEqualTo(1);
    }

    @Test
    void whenAttendeesLeaveConcurrentlyShouldKeepCountInSyncWithMembership() throws Exception {
        UUID eventId = testDataInitializer.setupFirstEvent();
        User firstAttendee = requirePresent(userRepository.findByIgnoreCaseEmail(UserConstants.SECOND_USER_EMAIL), "Expected user record in whenAttendeesLeaveConcurrentlyShouldKeepCountInSyncWithMembership");
        User secondAttendee = createConcurrentCandidate(firstAttendee);
        assertThat(attendAs(firstAttendee, eventId)).isTrue();
        assertThat(attendAs(secondAttendee, eventId)).isTrue();

        List<Boolean> outcomes = runConcurrently(
                () -> leaveAs(firstAttendee, eventId),
                () -> leaveAs(secondAttendee, eventId)
        );

        assertThat(outcomes).containsExactly(true, true);
        assertThat(requirePresent(eventRepository.findById(eventId), "Expected event record in whenAttendeesLeaveConcurrentlyShouldKeepCountInSyncWithMembership").getAttendeeCount()).isZero();
        assertThat(attendeeRows(eventId)).isZero();
        assertThat(eventRepository.findAttendeeIdsByEventId(eventId)).isEmpty();
    }

    @Test
    void whenCapacityReductionRacesAttendanceShouldPreserveCapacityInvariant() throws Exception {
        UUID eventId = testDataInitializer.setupFirstEvent();
        User owner = requirePresent(userRepository.findByIgnoreCaseEmail(UserConstants.FIRST_USER_EMAIL), "Expected user record in whenCapacityReductionRacesAttendanceShouldPreserveCapacityInvariant");
        User firstAttendee = requirePresent(userRepository.findByIgnoreCaseEmail(UserConstants.SECOND_USER_EMAIL), "Expected user record in whenCapacityReductionRacesAttendanceShouldPreserveCapacityInvariant");
        User secondAttendee = createConcurrentCandidate(firstAttendee);
        assertThat(attendAs(firstAttendee, eventId)).isTrue();
        Event originalEvent = requirePresent(eventRepository.findById(eventId),
                "Expected event with its first attendee before capacity race");
        originalEvent.setCreateDate(TimeConstants.ONE_WEEK_AGO);
        originalEvent.setLastUpdate(TimeConstants.ONE_HOUR_AGO);
        eventRepository.saveAndFlush(originalEvent);
        Integer originalCapacity = originalEvent.getMaxAttendees();
        List<String> originalTags = eventTagNames(eventId);
        EventCreateDto update = EventCreateDtoTestBuilder.updatedEvent()
                .maxAttendees(1)
                .build();

        List<Boolean> outcomes = runConcurrently(
                () -> updateAs(owner, eventId, update),
                () -> attendAs(secondAttendee, eventId)
        );

        Event storedEvent = requirePresent(eventRepository.findById(eventId), "Expected event record in whenCapacityReductionRacesAttendanceShouldPreserveCapacityInvariant");
        assertThat(outcomes).containsExactlyInAnyOrder(true, false);
        assertThat(storedEvent.getAttendeeCount()).isEqualTo(attendeeRows(eventId));
        if (outcomes.getFirst()) {
            assertThat(storedEvent.getMaxAttendees()).isEqualTo(1);
            assertThat(storedEvent.getAttendeeCount()).isEqualTo(1);
            assertThat(eventRepository.findAttendeeIdsByEventId(eventId)).containsExactly(firstAttendee.getId());
            assertThat(storedEvent).extracting(Event::getName, Event::getShortDescription,
                            Event::getLongDescription, Event::getExactAddress, Event::getEventStartDate,
                            Event::getCreateDate, Event::getLastUpdate, event -> event.getCity().getExternalId())
                    .containsExactly(update.getName(), update.getShortDescription(), update.getLongDescription(),
                            update.getExactAddress(), TimeConstants.EVENT_UPDATE_START_DATE,
                            TimeConstants.ONE_WEEK_AGO, TimeConstants.NOW, update.getCityExternalId());
            assertThat(eventTagNames(eventId)).containsExactlyInAnyOrderElementsOf(TagConstants.EVENT_UPDATE_TAGS);
            assertThat(eventNotificationRecipients(eventId)).containsExactly(firstAttendee.getId());
        } else {
            assertThat(storedEvent.getMaxAttendees()).isEqualTo(originalCapacity);
            assertThat(storedEvent.getAttendeeCount()).isEqualTo(2);
            assertThat(eventRepository.findAttendeeIdsByEventId(eventId))
                    .containsExactlyInAnyOrder(firstAttendee.getId(), secondAttendee.getId());
            assertThat(storedEvent).extracting(Event::getName, Event::getShortDescription,
                            Event::getLongDescription, Event::getExactAddress, Event::getEventStartDate,
                            Event::getCreateDate, Event::getLastUpdate, event -> event.getCity().getId())
                    .containsExactly(originalEvent.getName(), originalEvent.getShortDescription(),
                            originalEvent.getLongDescription(), originalEvent.getExactAddress(),
                            originalEvent.getEventStartDate(), TimeConstants.ONE_WEEK_AGO,
                            TimeConstants.ONE_HOUR_AGO, originalEvent.getCity().getId());
            assertThat(eventTagNames(eventId)).isEqualTo(originalTags);
            assertThat(eventNotificationRecipients(eventId)).isEmpty();
        }
        assertThat(storedEvent.getOwner().getId()).isEqualTo(owner.getId());
    }

    @Test
    void whenFiftiethFilesAreUploadedConcurrentlyShouldPersistExactlyOneFile() throws Exception {
        UUID eventId = testDataInitializer.setupFirstEvent();
        Event event = requirePresent(eventRepository.findById(eventId), "Expected event record in whenFiftiethFilesAreUploadedConcurrentlyShouldPersistExactlyOneFile");
        User owner = requirePresent(userRepository.findByIgnoreCaseEmail(UserConstants.FIRST_USER_EMAIL), "Expected user record in whenFiftiethFilesAreUploadedConcurrentlyShouldPersistExactlyOneFile");
        User attendee = requirePresent(userRepository.findByIgnoreCaseEmail(UserConstants.SECOND_USER_EMAIL), "Expected user record in whenFiftiethFilesAreUploadedConcurrentlyShouldPersistExactlyOneFile");
        testDataInitializer.addSecondUserToAttendees(eventId);

        for (int index = 0; index < 49; index++) {
            fileRepository.save(new FileTestBuilder()
                    .id(null)
                    .event(event)
                    .owner(owner)
                    .userFileName("seed-" + index)
                    .originalFileName("seed-" + index + ".jpg")
                    .contentType(FileConstants.JPG_FILE_CONTENT_TYPE)
                    .content(new byte[] {1})
                    .uploadDateTime(TimeConstants.NOW)
                    .build());
        }
        fileRepository.flush();
        List<UUID> seededIds = fileRepository.findAll().stream().map(File::getId).toList();
        assertThat(seededIds).hasSize(49).doesNotHaveDuplicates();

        List<Boolean> outcomes = runConcurrently(
                () -> uploadAs(owner, eventId, "owner-upload"),
                () -> uploadAs(attendee, eventId, "attendee-upload")
        );

        assertThat(outcomes).containsExactlyInAnyOrder(true, false);
        assertThat(fileRepository.countByEventId(eventId)).isEqualTo(50);
        List<File> storedFiles = fileRepository.findAll();
        assertThat(storedFiles).hasSize(50).extracting(File::getId).containsAll(seededIds);
        User winningUploader = outcomes.getFirst() ? owner : attendee;
        String winningName = outcomes.getFirst() ? "owner-upload" : "attendee-upload";
        assertThat(storedFiles.stream().filter(file -> !seededIds.contains(file.getId())).toList())
                .singleElement().satisfies(file -> {
                    assertThat(file.getId()).isNotNull();
                    assertThat(file.getOwner().getId()).isEqualTo(winningUploader.getId());
                    assertThat(file.getEvent().getId()).isEqualTo(eventId);
                    assertThat(file.getUserFileName()).isEqualTo(winningName);
                    assertThat(file.getContentType()).isEqualTo(FileConstants.JPG_FILE_CONTENT_TYPE);
                    assertThat(file.getUploadDateTime()).isEqualTo(TimeConstants.NOW);
                });
    }

    private boolean attendAs(User user, UUID eventId) {
        try {
            authenticate(user);
            eventService.addAttendeeToEvent(eventId);
            return true;
        } catch (EventCapacityReachedException exception) {
            return false;
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private boolean leaveAs(User user, UUID eventId) {
        try {
            authenticate(user);
            eventService.removeAttendeeFromEvent(eventId);
            return true;
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private boolean updateAs(User owner, UUID eventId, EventCreateDto update) {
        try {
            authenticate(owner);
            eventService.updateEvent(update, eventId);
            return true;
        } catch (EventCapacityTooSmallException exception) {
            return false;
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private User createConcurrentCandidate(User template) {
        return userRepository.save(new UserTestBuilder()
                .id(null)
                .roles(java.util.Set.of())
                .firstName("Concurrent")
                .lastName("Candidate")
                .email("concurrent.candidate@example.com")
                .homeCity(template.getHomeCity())
                .timeZone(template.getTimeZone())
                .build());
    }

    private int attendeeRows(UUID eventId) {
        return jdbcTemplate.queryForObject(
                "select count(*) from event_user where event_id = ?", Integer.class, eventId);
    }

    private List<String> eventTagNames(UUID eventId) {
        return jdbcTemplate.queryForList("""
                SELECT t.name FROM tags t JOIN event_tag et ON t.id = et.tag_id
                WHERE et.event_id = ? ORDER BY t.name
                """, String.class, eventId);
    }

    private List<UUID> eventNotificationRecipients(UUID eventId) {
        return jdbcTemplate.queryForList("""
                SELECT recipient_id FROM notifications WHERE resource_type = 'EVENT' AND resource_id = ?
                """, UUID.class, eventId);
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
        JwtUserDetails principal = new JwtUserDetailsTestBuilder()
                .id(user.getId())
                .email(user.getEmail())
                .authorities(user.getRoles().stream().map(role -> new SimpleGrantedAuthority(role.getName())).toList())
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }

    private <T> List<T> runConcurrently(ThrowingSupplier<T> first, ThrowingSupplier<T> second) throws Exception {
        ExecutorService executor = TestWorkers.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<T> firstResult = executor.submit(() -> awaitAndRun(ready, start, first));
            Future<T> secondResult = executor.submit(() -> awaitAndRun(ready, start, second));
            assertThat(ready.await(5, TimeUnit.SECONDS))
                    .as("Community workers reached the start barrier").isTrue();
            start.countDown();
            return List.of(firstResult.get(10, TimeUnit.SECONDS), secondResult.get(10, TimeUnit.SECONDS));
        } finally {
            start.countDown();
            TestWorkers.stop(executor);
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
