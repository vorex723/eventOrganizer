package com.mazurek.eventOrganizer.common;

import com.mazurek.eventOrganizer.city.City;
import com.mazurek.eventOrganizer.event.Event;
import com.mazurek.eventOrganizer.event.EventRepository;
import com.mazurek.eventOrganizer.event.EventService;
import com.mazurek.eventOrganizer.file.File;
import com.mazurek.eventOrganizer.file.FileOverviewDto;
import com.mazurek.eventOrganizer.file.FileService;
import com.mazurek.eventOrganizer.jwt.JwtUserDetails;
import com.mazurek.eventOrganizer.notification.domain.Notification;
import com.mazurek.eventOrganizer.testData.builders.EventTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.FileTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.ThreadReplyTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.ThreadTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.UserTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.JwtUserDetailsTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.dto.EventCreateDtoTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.dto.FileUploadDtoTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.dto.ThreadCreateDtoTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.dto.ThreadReplyCreateDtoTestBuilder;
import com.mazurek.eventOrganizer.thread.Thread;
import com.mazurek.eventOrganizer.thread.ThreadService;
import com.mazurek.eventOrganizer.thread.ThreadSortField;
import com.mazurek.eventOrganizer.thread.dto.ThreadDto;
import com.mazurek.eventOrganizer.threadReply.ThreadReply;
import com.mazurek.eventOrganizer.threadReply.ThreadReplyService;
import com.mazurek.eventOrganizer.user.User;
import com.mazurek.eventOrganizer.testSupport.database.SqlCapture;
import com.mazurek.eventOrganizer.testSupport.database.SqlInspectionConfiguration;
import jakarta.persistence.EntityManager;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Import(SqlInspectionConfiguration.class)
@DisplayName("Event access and notification recipient loading tests:")
class EventAccessAndRecipientsIntegrationTest {

    @Autowired private EntityManager entityManager;
    @Autowired private EventRepository eventRepository;
    @Autowired private ThreadService threadService;
    @Autowired private ThreadReplyService threadReplyService;
    @Autowired private FileService fileService;
    @Autowired private EventService eventService;
    @Autowired private Clock clock;
    @Autowired private SqlCapture sqlCapture;

    @org.junit.jupiter.api.BeforeEach
    void resetAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @ParameterizedTest(name = "{0} attendees")
    @ValueSource(ints = {0, 1, 20, 50})
    @DisplayName("Repository access checks and UUID queries should not hydrate attendees")
    void whenQueryingAccessAndAttendeeIdsShouldNotHydrateUsers(int attendeeCount) {
        Graph graph = persistGraph(attendeeCount);
        Event event = entityManager.find(Event.class, graph.eventId());

        assertThat(eventRepository.existsById(graph.eventId())).isTrue();
        assertThat(eventRepository.isUserAttendeeOrOwner(graph.ownerId(), graph.eventId())).isTrue();
        assertThat(eventRepository.isUserAttendeeOrOwner(UUID.randomUUID(), graph.eventId())).isFalse();
        if (!graph.attendeeIds().isEmpty()) {
            assertThat(eventRepository.isUserAttendeeOrOwner(graph.attendeeIds().getFirst(), graph.eventId())).isTrue();
        }
        assertThat(eventRepository.findAttendeeIdsByEventId(graph.eventId()))
                .containsExactlyInAnyOrderElementsOf(graph.attendeeIds());

        UUID missingEventId = UUID.randomUUID();
        assertThat(eventRepository.existsById(missingEventId)).isFalse();
        assertThat(eventRepository.isUserAttendeeOrOwner(graph.ownerId(), missingEventId)).isFalse();
        assertThat(eventRepository.findAttendeeIdsByEventId(missingEventId)).isEmpty();
        assertAttendeesNotLoaded(event, graph);
    }

    enum Flow {
        FILE_OVERVIEW, FILE_CONTENT, FILE_PAGE, THREAD_PAGE, THREAD_READ, THREAD_UPDATE,
        REPLY_PAGE, REPLY_CREATE, REPLY_UPDATE, THREAD_CREATE, FILE_UPLOAD, EVENT_UPDATE
    }

    static Stream<Arguments> flowsAndAttendeeCounts() {
        return Arrays.stream(Flow.values()).flatMap(flow ->
                IntStream.of(0, 1, 20, 50).mapToObj(count -> Arguments.of(flow, count)));
    }

    @ParameterizedTest(name = "{0}, {1} attendees")
    @MethodSource("flowsAndAttendeeCounts")
    @DisplayName("Community operations should not initialize attendees or hydrate recipient users")
    void whenMutatingEventShouldNotHydrateAttendees(Flow flow, int attendeeCount) {
        Graph graph = persistGraph(attendeeCount);
        Event event = entityManager.find(Event.class, graph.eventId());
        authenticate(graph.ownerId());
        assertAttendeesNotLoaded(event, graph);

        List<String> statements = sqlCapture.capture(() -> {
            perform(flow, graph);
            entityManager.flush();
        });

        assertAttendeesNotLoaded(event, graph);
        // Scalar membership/UUID queries may read event_user, but must not select full participant data.
        assertThat(statements).noneMatch(sql -> sql.toLowerCase().contains("join event_user")
                && (sql.toLowerCase().contains("first_name") || sql.toLowerCase().contains("password")));

        if (flow == Flow.THREAD_CREATE || flow == Flow.FILE_UPLOAD || flow == Flow.EVENT_UPDATE) {
            List<Notification> notifications = entityManager.createQuery("""
                    SELECT notification FROM Notification notification
                    WHERE notification.resourceId = :eventId OR notification.parentResourceId = :eventId
                    """, Notification.class).setParameter("eventId", graph.eventId()).getResultList();
            assertThat(notifications).extracting(Notification::getRecipientId)
                    .containsExactlyInAnyOrderElementsOf(graph.attendeeIds());
        }
    }

    private void perform(Flow flow, Graph graph) {
        switch (flow) {
            case FILE_OVERVIEW -> fileService.getFileOverviewById(graph.fileId(), graph.eventId());
            case FILE_CONTENT -> fileService.getFileDataById(graph.fileId(), graph.eventId());
            case FILE_PAGE -> fileService.getFileOverviewPageByEventId(graph.eventId(), 0);
            case THREAD_PAGE -> threadService.getThreadsByEventId(graph.eventId(), 0,
                    ThreadSortField.LAST_ACTIVITY, SortDirection.DESC);
            case THREAD_READ -> threadService.getThreadInEvent(graph.eventId(), graph.threadId());
            case THREAD_UPDATE -> threadService.updateThreadInEvent(ThreadCreateDtoTestBuilder.firstThreadUpdate().build(),
                    graph.eventId(), graph.threadId());
            case REPLY_PAGE -> threadReplyService.getRepliesInEventThread(graph.eventId(), graph.threadId(), 0);
            case REPLY_CREATE -> threadReplyService.createReplyInThread(ThreadReplyCreateDtoTestBuilder.firstReply().build(),
                    graph.eventId(), graph.threadId());
            case REPLY_UPDATE -> threadReplyService.updateThreadReplyInEventThread(
                    ThreadReplyCreateDtoTestBuilder.firstReplyUpdate().build(),
                    graph.eventId(), graph.threadId(), graph.replyId());
            case THREAD_CREATE -> {
                ThreadDto created = threadService.createThreadInEvent(ThreadCreateDtoTestBuilder.firstThread().build(), graph.eventId());
                assertThat(entityManager.find(Thread.class, created.getId()).getEvent().getId()).isEqualTo(graph.eventId());
            }
            case FILE_UPLOAD -> {
                try {
                    FileOverviewDto created = fileService.uploadFileToEvent(FileUploadDtoTestBuilder.jpgFile().build(), graph.eventId());
                    assertThat(entityManager.find(File.class, created.getId()).getEvent().getId()).isEqualTo(graph.eventId());
                } catch (IOException exception) {
                    throw new UncheckedIOException(exception);
                }
            }
            case EVENT_UPDATE -> eventService.updateEvent(EventCreateDtoTestBuilder.updatedEvent()
                    .cityExternalId(com.mazurek.eventOrganizer.testData.TestCityData.externalId(graph.cityName())).tags(Set.of())
                    .eventStartDate(clock.instant().plus(30, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MINUTES))
                    .build(), graph.eventId());
        }
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = Flow.class, names = {"THREAD_CREATE", "FILE_UPLOAD"})
    @DisplayName("Attendee-created content should notify the owner and other attendees but not its author")
    void whenNotifyingEventRecipientsShouldIncludeOwnerWithoutHydration(Flow flow) {
        Graph graph = persistGraph(2);
        assertThat(graph.attendeeIds()).hasSize(2);
        UUID authorId = graph.attendeeIds().getFirst();
        UUID otherAttendeeId = graph.attendeeIds().getLast();
        Event event = entityManager.find(Event.class, graph.eventId());
        authenticate(authorId);

        perform(flow, graph);
        entityManager.flush();

        assertThat(Hibernate.isInitialized(event.getAttendees())).isFalse();
        assertThat(Hibernate.isInitialized(entityManager.getReference(User.class, otherAttendeeId))).isFalse();
        List<Notification> notifications = entityManager.createQuery("""
                SELECT notification FROM Notification notification
                WHERE notification.parentResourceId = :eventId
                """, Notification.class).setParameter("eventId", graph.eventId()).getResultList();
        assertThat(notifications).extracting(Notification::getRecipientId)
                .containsExactlyInAnyOrder(graph.ownerId(), otherAttendeeId);
    }

    private void assertAttendeesNotLoaded(Event event, Graph graph) {
        assertThat(Hibernate.isInitialized(event.getAttendees())).isFalse();
        for (UUID attendeeId : graph.attendeeIds()) {
            assertThat(Hibernate.isInitialized(entityManager.getReference(User.class, attendeeId)))
                    .as("Attendee %s must not be hydrated", attendeeId).isFalse();
        }
    }

    private void authenticate(UUID userId) {
        JwtUserDetails principal = new JwtUserDetailsTestBuilder().id(userId).email("owner@example.com")
                .authorities(List.of()).build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    private Graph persistGraph(int attendeeCount) {
        String cityName = "access city " + UUID.randomUUID().toString().substring(0, 8);
        City city = com.mazurek.eventOrganizer.testData.builders.CityTestBuilder.warsaw().name(cityName)
                .externalId(com.mazurek.eventOrganizer.testData.TestCityData.externalId(cityName)).id(null).build();
        entityManager.persist(city);
        User owner = persistUser(city);
        Event event = EventTestBuilder.firstEvent().id(null).owner(owner).city(city)
                .eventStartDate(clock.instant().plus(7, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MINUTES)).build();
        List<UUID> attendeeIds = new ArrayList<>();
        for (int index = 0; index < attendeeCount; index++) {
            User attendee = persistUser(city);
            event.addAttendee(attendee);
            attendeeIds.add(attendee.getId());
        }
        entityManager.persist(event);
        Thread thread = ThreadTestBuilder.firstThread().id(null).event(event).owner(owner).build();
        entityManager.persist(thread);
        ThreadReply reply = ThreadReplyTestBuilder.firstReply().id(null).thread(thread).replier(owner).build();
        thread.addReplyToThread(reply);
        entityManager.persist(reply);
        File file = FileTestBuilder.jpgFile().id(null).event(event).owner(owner).build();
        event.addFile(file);
        entityManager.persist(file);
        entityManager.flush();
        Graph graph = new Graph(event.getId(), owner.getId(), thread.getId(), reply.getId(), file.getId(),
                city.getName(), List.copyOf(attendeeIds));
        entityManager.clear();
        return graph;
    }

    private User persistUser(City city) {
        User user = UserTestBuilder.firstUser().id(null).homeCity(city).roles(Set.of())
                .email("access-" + UUID.randomUUID() + "@example.com").build();
        entityManager.persist(user);
        return user;
    }

    private record Graph(UUID eventId, UUID ownerId, UUID threadId, UUID replyId, UUID fileId,
                         String cityName, List<UUID> attendeeIds) {}
}
