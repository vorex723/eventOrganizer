package com.mazurek.eventOrganizer.config.seed;

import com.mazurek.eventOrganizer.auth.AuthUserLockService;
import com.mazurek.eventOrganizer.city.City;
import com.mazurek.eventOrganizer.conversation.Conversation;
import com.mazurek.eventOrganizer.conversation.ConversationType;
import com.mazurek.eventOrganizer.conversation.direct.DirectConversationPair;
import com.mazurek.eventOrganizer.conversation.message.Message;
import com.mazurek.eventOrganizer.conversation.participant.ConversationParticipant;
import com.mazurek.eventOrganizer.event.Event;
import com.mazurek.eventOrganizer.file.File;
import com.mazurek.eventOrganizer.notification.domain.*;
import com.mazurek.eventOrganizer.tag.Tag;
import com.mazurek.eventOrganizer.tag.TagService;
import com.mazurek.eventOrganizer.thread.Thread;
import com.mazurek.eventOrganizer.threadReply.ThreadReply;
import com.mazurek.eventOrganizer.user.*;
import com.mazurek.eventOrganizer.utils.EncryptionUtils;
import com.mazurek.eventOrganizer.utils.FileUtils;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Local-only fixtures, deliberately bypassing authenticated commands and delivery/outbox services.
 * Reserved names/markers identify examples; generated entity IDs remain owned by Hibernate.
 * Run on one local application instance, not concurrently from multiple seeders.
 */
@Component
@Profile("local")
@RequiredArgsConstructor
public class LocalDemoSeeder {
    private static final List<AccountSpec> ACCOUNTS = List.of(
            new AccountSpec("normal", "Normal", "Normal123@", List.of("ROLE_USER")),
            new AccountSpec("admin", "Admin", "Admin123@", List.of("ROLE_USER", "ROLE_ADMIN")),
            new AccountSpec("participant", "Participant", "Participant123@", List.of("ROLE_USER")),
            new AccountSpec("moderator", "Moderator", "Moderator123@", List.of("ROLE_USER", "ROLE_MODERATOR")));
    private static final List<EventSpec> EVENTS = List.of(
            new EventSpec("[DEMO] Java community meetup", 7, null, 0, "demo-java", "demo-community"),
            new EventSpec("[DEMO] Outdoor workshop (full)", 14, 3, 1, "demo-outdoor", "demo-workshop"),
            new EventSpec("[DEMO] Community retrospective (past)", -7, 10, 3, "demo-community", "demo-workshop"));
    private static final List<PreferenceSpec> PREFERENCES = List.of(
            new PreferenceSpec(NotificationResourceType.EVENT, NotificationChannel.EMAIL),
            new PreferenceSpec(NotificationResourceType.THREAD, NotificationChannel.PUSH_WEB),
            new PreferenceSpec(NotificationResourceType.CONVERSATION, NotificationChannel.PUSH_MOBILE));

    private final EntityManager entityManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthUserLockService userLockService;
    private final TagService tagService;
    private final EncryptionUtils encryptionUtils;
    private final FileUtils fileUtils;
    private final Clock clock;

    @Transactional
    public LocalSeedReport seed(City seedCity) {
        Instant anchor = clock.instant();
        City city = entityManager.find(City.class, seedCity.getId());
        if (city == null) {
            throw new IllegalArgumentException("Demo seed requires a persisted city");
        }
        List<User> users = ACCOUNTS.stream().map(spec -> ensureUser(spec, city, anchor)).toList();
        entityManager.flush();
        List<LocalSeedReport.Resource> resources = new ArrayList<>();
        link(resources, city.getName(), List.of(), "cities", city.getId());
        link(resources, "City events", List.of(), "cities", city.getId(), "events");
        Set<Tag> tags = tagService.getTagsByNames(Set.of("demo-java", "demo-outdoor", "demo-workshop", "demo-community"));
        tags.stream().sorted(Comparator.comparing(Tag::getName)).forEach(tag -> {
            link(resources, tag.getName(), List.of(), "tags", tag.getName());
            link(resources, "Tag events", List.of(), "tags", tag.getName(), "events");
        });
        List<Event> events = new ArrayList<>();
        for (int index = 0; index < EVENTS.size(); index++) {
            Event event = ensureEvent(EVENTS.get(index), city, users, tags, anchor);
            events.add(event);
            seedCommunity(event, users, index, resources);
        }
        List<Conversation> conversations = List.of(
                ensureDirect(users.get(0), users.get(1), anchor),
                ensureDirect(users.get(0), users.get(3), anchor),
                ensureGroup(users, anchor));
        for (Conversation conversation : conversations) {
            seedMessages(conversation);
            List<String> access = conversation.getParticipants().stream()
                    .filter(participant -> participant.getUser() != null && participant.getLeftAt() == null)
                    .map(participant -> participant.getUser().getEmail()).sorted().toList();
            protectedLink(resources, "Conversation " + conversation.getId(), access, "conversations", conversation.getId());
            protectedLink(resources, "Conversation messages", access, "conversations", conversation.getId(), "messages");
        }
        for (User user : users) {
            seedNotifications(user, events, conversations.get(2), anchor);
            seedPreferences(user);
            List<String> self = List.of(user.getEmail());
            link(resources, "User " + user.getEmail(), self, "users", user.getId());
            link(resources, "Notifications for " + user.getEmail(), self, "notifications");
            link(resources, "Unread count for " + user.getEmail(), self, "notifications", "unread-count");
            link(resources, "Preferences for " + user.getEmail(), self, "notifications", "preferences");
        }
        entityManager.flush();
        return new LocalSeedReport(users.stream().map(user -> new LocalSeedReport.Account(user.getId(), user.getEmail(),
                user.getRoles().stream().map(Role::getName).sorted().toList())).toList(), resources);
    }

    private User ensureUser(AccountSpec spec, City city, Instant anchor) {
        return userRepository.findByIgnoreCaseEmail(spec.email()).orElseGet(() -> {
            Set<Role> roles = new HashSet<>();
            for (String name : spec.roles()) {
                roles.add(roleRepository.findByName(name).orElseThrow(() -> new IllegalStateException("Missing demo role: " + name)));
            }
            Instant created = anchor.minus(30, ChronoUnit.DAYS);
            User user = User.builder().email(spec.email()).firstName(spec.firstName()).lastName("User")
                    .password(passwordEncoder.encode(spec.password())).roles(roles).homeCity(city)
                    .timeZone(city.getTimeZoneId()).createdAt(created).lastCredentialsChangeTime(created)
                    .activated(true).banned(false).build();
            entityManager.persist(user);
            return user;
        });
    }

    private Event ensureEvent(EventSpec spec, City city, List<User> users, Set<Tag> tags, Instant anchor) {
        Event event = entityManager.createQuery("select e from Event e where e.name = :name", Event.class)
                .setParameter("name", spec.name()).getResultStream().findFirst().orElseGet(() -> {
                    Instant created = anchor.minus(10, ChronoUnit.DAYS);
                    Event createdEvent = Event.builder().name(spec.name())
                            .shortDescription("Local demo: " + spec.firstTag())
                            .longDescription("Sample event for exploring attendees, discussions and downloadable files.")
                            .city(city).exactAddress("Demo meeting point, Main Street 1").owner(users.get(spec.ownerIndex()))
                            .createDate(created).lastUpdate(created).eventStartDate(anchor.plus(spec.offsetDays(), ChronoUnit.DAYS))
                            .maxAttendees(spec.limit()).build();
                    entityManager.persist(createdEvent);
                    return createdEvent;
                });
        users.stream().filter(user -> !user.equals(event.getOwner())).forEach(event::addAttendee);
        // Includes user-added participants; corrects stale counters after manual deletions too.
        event.setAttendeeCount(event.getAttendees().size());
        tags.stream().filter(tag -> Set.of(spec.firstTag(), spec.secondTag()).contains(tag.getName())).forEach(event::addTag);
        return event;
    }

    private void seedCommunity(Event event, List<User> users, int index, List<LocalSeedReport.Resource> resources) {
        List<String> access = users.stream().filter(event::isUserAttendeeOrOwner).map(User::getEmail).toList();
        link(resources, event.getName(), List.of(), "events", event.getId());
        protectedLink(resources, "Attendees", access, "events", event.getId(), "attendees");
        link(resources, "Threads", access, "events", event.getId(), "threads");
        for (int threadIndex = 1; threadIndex <= 2; threadIndex++) {
            String name = "[DEMO] " + (threadIndex == 1 ? "Introductions" : "Agenda and logistics");
            Instant created = event.getCreateDate().plus(threadIndex, ChronoUnit.HOURS);
            Thread thread = event.getThreads().stream().filter(value -> value.getName().equals(name)).findFirst().orElse(null);
            if (thread == null) {
                thread = Thread.builder().name(name).content("A demo discussion. Share ideas and questions here.")
                        .owner(users.get(threadIndex - 1)).createDate(created).lastUpdate(created).lastActivity(created).build();
                event.addThread(thread);
                entityManager.persist(thread);
            }
            for (int replyIndex = 1; replyIndex <= 3; replyIndex++) {
                String marker = "[DEMO reply " + replyIndex + "]";
                if (thread.getReplies().stream().noneMatch(reply -> reply.getContent().startsWith(marker))) {
                    Instant date = thread.getCreateDate().plus(replyIndex, ChronoUnit.MINUTES);
                    ThreadReply reply = ThreadReply.builder().content(marker + " Thanks, looking forward to the discussion!")
                            .replier(users.get(replyIndex)).replyDate(date).lastUpdate(date).build();
                    thread.addReplyToThread(reply);
                    entityManager.persist(reply);
                }
            }
            thread.setReplyCount(thread.getReplies().size());
            // Do not move activity backwards; include any replies added outside the demo.
            for (ThreadReply reply : thread.getReplies()) {
                thread.setLastActivity(later(thread.getLastActivity(), later(reply.getReplyDate(), reply.getLastUpdate())));
            }
            link(resources, thread.getName(), access, "events", event.getId(), "threads", thread.getId());
            link(resources, "Replies " + thread.getReplies().stream().map(ThreadReply::getId).sorted().toList(),
                    access, "events", event.getId(), "threads", thread.getId(), "replies");
        }
        String filename = index == 1 ? "demo-map.png" : "demo-agenda-" + (index + 1) + ".pdf";
        File file = event.getFiles().stream().filter(value -> value.getOriginalFileName().equals(filename)).findFirst().orElse(null);
        if (file == null) {
            byte[] bytes = index == 1 ? LocalSeedFiles.png() : LocalSeedFiles.pdf("Event Organizer - Demo agenda " + (index + 1));
            String mime = fileUtils.detectValidatedContentType(filename, bytes)
                    .orElseThrow(() -> new IllegalStateException("Invalid local demo file"));
            file = File.builder().originalFileName(filename).userFileName(filename).content(bytes).contentType(mime)
                    .owner(users.get(0)).uploadDateTime(event.getCreateDate().plus(3, ChronoUnit.HOURS)).build();
            event.addFile(file);
            entityManager.persist(file);
        }
        link(resources, "Files", access, "events", event.getId(), "files");
        link(resources, file.getUserFileName(), access, "events", event.getId(), "files", file.getId());
        link(resources, "Download " + file.getUserFileName(), access, "events", event.getId(), "files", file.getId(), "data");
    }

    private Conversation ensureDirect(User first, User second, Instant anchor) {
        var ids = DirectConversationPair.DirectPairIds.of(first.getId(), second.getId());
        // Unlike the production active-pair query, this also finds conversations someone left.
        Conversation conversation = entityManager.createQuery(
                        "select p.conversation from DirectConversationPair p where p.firstUserId = :first and p.secondUserId = :second",
                        Conversation.class).setParameter("first", ids.firstUserId()).setParameter("second", ids.secondUserId())
                .getResultStream().findFirst().orElse(null);
        if (conversation == null) {
            conversation = newConversation(ConversationType.DIRECT, null, anchor);
            entityManager.persist(DirectConversationPair.of(conversation, first.getId(), second.getId()));
        }
        ensureParticipants(conversation, List.of(first, second));
        return conversation;
    }

    private Conversation ensureGroup(List<User> users, Instant anchor) {
        String name = "[DEMO] Event Organizer team";
        Conversation conversation = entityManager.createQuery(
                        "select c from Conversation c where c.type = :type and c.name = :name", Conversation.class)
                .setParameter("type", ConversationType.GROUP).setParameter("name", name)
                .getResultStream().findFirst().orElseGet(() -> newConversation(ConversationType.GROUP, name, anchor));
        ensureParticipants(conversation, users);
        return conversation;
    }

    private Conversation newConversation(ConversationType type, String name, Instant anchor) {
        Instant created = anchor.minus(3, ChronoUnit.DAYS);
        Conversation conversation = Conversation.builder().type(type).name(name).createdAt(created).lastActiveAt(created).build();
        entityManager.persist(conversation);
        return conversation;
    }

    private void ensureParticipants(Conversation conversation, List<User> users) {
        for (User user : users) {
            if (conversation.getParticipants().stream().noneMatch(participant -> user.equals(participant.getUser()))) {
                var participant = ConversationParticipant.builder().user(user).userNameAtJoin(user.getFullName())
                        .joinedAt(conversation.getCreatedAt()).build();
                conversation.addParticipant(participant);
                entityManager.persist(participant);
            }
        }
    }

    private void seedMessages(Conversation conversation) {
        List<Message> messages = entityManager.createQuery(
                        "select m from Message m where m.conversation = :conversation order by m.id", Message.class)
                .setParameter("conversation", conversation).getResultList();
        List<String> plaintexts = messages.stream().map(message -> encryptionUtils
                .decryptConversationMessage(message.getContent(), message.getEncryptionKeyId()).orElseThrow(() ->
                        new IllegalStateException("Cannot decrypt existing demo conversation " + conversation.getId()
                                + "; restore its message encryption keys before seeding."))).toList();
        List<User> senders = conversation.getParticipants().stream()
                .filter(participant -> participant.getUser() != null && participant.getLeftAt() == null)
                .map(ConversationParticipant::getUser).sorted(Comparator.comparing(User::getEmail)).toList();
        for (int index = 1; index <= 4; index++) {
            String marker = "[DEMO message " + index + "]";
            if (plaintexts.stream().noneMatch(content -> content.startsWith(marker))) {
                if (senders.isEmpty()) {
                    throw new IllegalStateException("Cannot add demo messages to a conversation with no active participants: " + conversation.getId());
                }
                var encrypted = encryptionUtils.encryptConversationMessage(marker + " Let's organize our next meetup.");
                User sender = senders.get((index - 1) % senders.size());
                Instant sentAt = conversation.getCreatedAt().plus(index, ChronoUnit.MINUTES);
                Message message = new Message(sender, sender.getFullName(), encrypted.keyId(), encrypted.ciphertext(), sentAt, conversation);
                entityManager.persist(message);
                messages.add(message);
            }
        }
        for (Message message : messages) {
            conversation.setLastActiveAt(later(conversation.getLastActiveAt(), message.getSentDate()));
        }
    }

    private void seedNotifications(User user, List<Event> events, Conversation group, Instant anchor) {
        Event event = events.get(0);
        Thread thread = event.getThreads().stream().filter(value -> value.getName().equals("[DEMO] Introductions")).findFirst().orElseThrow();
        List<NoticeSpec> notices = List.of(
                new NoticeSpec("[DEMO] Welcome to the meetup", NotificationResourceType.EVENT, event.getId(), null, null),
                new NoticeSpec("[DEMO] Join the discussion", NotificationResourceType.THREAD, thread.getId(), NotificationResourceType.EVENT, event.getId()),
                new NoticeSpec("[DEMO] Team conversation", NotificationResourceType.CONVERSATION, group.getId(), null, null));
        for (int index = 0; index < notices.size(); index++) {
            NoticeSpec spec = notices.get(index);
            Long count = entityManager.createQuery(
                            "select count(n) from Notification n where n.recipientId = :user and n.title = :title and n.resourceId = :resource",
                            Long.class).setParameter("user", user.getId()).setParameter("title", spec.title())
                    .setParameter("resource", spec.resourceId()).getSingleResult();
            if (count == 0) {
                entityManager.persist(Notification.builder().recipientId(user.getId()).title(spec.title())
                        .body("Local demo notification. Open the linked resource to explore it.")
                        .resourceType(spec.type()).resourceId(spec.resourceId()).parentResourceType(spec.parentType())
                        .parentResourceId(spec.parentId()).createdAt(anchor.minus(2, ChronoUnit.HOURS))
                        .readAt(index == 0 ? anchor.minus(1, ChronoUnit.HOURS) : null).build());
            }
        }
    }

    private void seedPreferences(User original) {
        // Lock and refresh BEFORE editing the user's preference version, never a stale full-row write.
        User user = userLockService.lockById(original.getId()).orElseThrow();
        boolean inserted = false;
        for (PreferenceSpec spec : PREFERENCES) {
            Long count = entityManager.createQuery(
                            "select count(p) from NotificationPreference p where p.userId = :user and p.resourceType = :type and p.channel = :channel",
                            Long.class).setParameter("user", user.getId()).setParameter("type", spec.type())
                    .setParameter("channel", spec.channel()).getSingleResult();
            if (count == 0) {
                entityManager.persist(NotificationPreference.builder().userId(user.getId()).resourceType(spec.type())
                        .channel(spec.channel()).enabled(false).build());
                inserted = true;
            }
        }
        if (inserted) {
            user.setNotificationPreferencesVersion(user.getNotificationPreferencesVersion() + 1);
        }
    }

    private static Instant later(Instant first, Instant second) {
        return first.isAfter(second) ? first : second;
    }

    private static void link(List<LocalSeedReport.Resource> resources, String label, List<String> accounts, Object... segments) {
        resources.add(new LocalSeedReport.Resource(label, path(segments), accounts));
    }

    private static void protectedLink(List<LocalSeedReport.Resource> resources, String label, List<String> accounts, Object... segments) {
        resources.add(new LocalSeedReport.Resource(label, path(segments), accounts, true));
    }

    private static String path(Object... segments) {
        return UriComponentsBuilder.fromPath("/api/v1")
                .pathSegment(Arrays.stream(segments).map(Object::toString).toArray(String[]::new)).build().encode().toUriString();
    }

    private record AccountSpec(String handle, String firstName, String password, List<String> roles) {
        String email() { return handle + "@eventorganizer.com"; }
    }
    private record EventSpec(String name, int offsetDays, Integer limit, int ownerIndex, String firstTag, String secondTag) {}
    private record PreferenceSpec(NotificationResourceType type, NotificationChannel channel) {}
    private record NoticeSpec(String title, NotificationResourceType type, UUID resourceId,
                              NotificationResourceType parentType, UUID parentId) {}
}
