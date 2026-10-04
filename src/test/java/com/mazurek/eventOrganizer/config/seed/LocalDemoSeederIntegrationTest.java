package com.mazurek.eventOrganizer.config.seed;

import com.mazurek.eventOrganizer.city.City;
import com.mazurek.eventOrganizer.auth.AuthUserLockService;
import com.mazurek.eventOrganizer.config.properties.EncryptionProperties;
import com.mazurek.eventOrganizer.conversation.Conversation;
import com.mazurek.eventOrganizer.conversation.message.Message;
import com.mazurek.eventOrganizer.conversation.participant.ConversationParticipant;
import com.mazurek.eventOrganizer.event.Event;
import com.mazurek.eventOrganizer.notification.domain.Notification;
import com.mazurek.eventOrganizer.notification.domain.NotificationPreference;
import com.mazurek.eventOrganizer.thread.Thread;
import com.mazurek.eventOrganizer.threadReply.ThreadReply;
import com.mazurek.eventOrganizer.user.User;
import com.mazurek.eventOrganizer.user.UserRepository;
import com.mazurek.eventOrganizer.user.RoleRepository;
import com.mazurek.eventOrganizer.tag.TagService;
import com.mazurek.eventOrganizer.utils.FileUtils;
import com.mazurek.eventOrganizer.utils.EncryptionUtils;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.Clock;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Committed seeds on an owned schema, not the developer database or shared test fixtures. */
@SpringBootTest(properties = "app.seed.local-data-enabled=false")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(LocalDemoSeederIntegrationTest.SeederConfiguration.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class LocalDemoSeederIntegrationTest {
    // Keep offline providers only: enabling both profiles selects two real/test email adapters.
    // A factory exposes the production seeder to the test profile without relaxing its local-only guard.
    @TestConfiguration
    static class SeederConfiguration {
        @Bean
        LocalDemoSeeder demoSeeder(EntityManager entityManager, UserRepository users, RoleRepository roles,
                                   PasswordEncoder encoder, AuthUserLockService locks, TagService tags,
                                   EncryptionUtils encryption, FileUtils files, Clock clock) {
            return new LocalDemoSeeder(entityManager, users, roles, encoder, locks, tags, encryption, files, clock);
        }
    }
    private static final String SCHEMA = "local_seed_" + UUID.randomUUID().toString().replace("-", "");
    private static final List<String> TABLES = List.of("cities", "users", "tags", "events", "event_user", "event_tag",
            "threads", "thread_replies", "files", "conversations", "conversation_participant", "direct_conversation_pair",
            "messages", "notifications", "notification_preferences");
    private static final List<String> EMPTY_TABLES = List.of("activation_tokens", "password_reset_tokens", "email_change_tokens",
            "refresh_tokens", "auth_email_deliveries", "notification_devices", "notification_deliveries");
    private static final Map<String, String> PASSWORDS = Map.of(
            "normal@eventorganizer.com", "Normal123@", "admin@eventorganizer.com", "Admin123@",
            "participant@eventorganizer.com", "Participant123@", "moderator@eventorganizer.com", "Moderator123@");

    @DynamicPropertySource
    static void isolatedSchema(DynamicPropertyRegistry properties) {
        properties.add("spring.flyway.schemas", () -> SCHEMA);
        properties.add("spring.flyway.default-schema", () -> SCHEMA);
        properties.add("spring.datasource.hikari.schema", () -> SCHEMA);
    }

    @Autowired private LocalDemoSeeder seeder;
    @Autowired private UserRepository users;
    @Autowired private EntityManager entityManager;
    @Autowired private TransactionTemplate transactions;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private EncryptionUtils encryption;
    @Autowired private EncryptionProperties encryptionProperties;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;
    private City city;

    @BeforeEach
    void resetOnlyOwnedDemoSchema() {
        assertThat(SCHEMA).matches("local_seed_[a-f0-9]{32}");
        List<String> tables = new ArrayList<>(TABLES);
        tables.addAll(EMPTY_TABLES);
        jdbc.execute("TRUNCATE TABLE " + String.join(", ", tables.stream().map(table -> SCHEMA + "." + table).toList()) + " CASCADE");
        city = transactions.execute(status -> {
            City created = new City("test:local-demo", "New York", "US", "New York", 40.7128, -74.006, "America/New_York");
            entityManager.persist(created);
            return created;
        });
    }

    @AfterAll
    void removeOwnedSchema() {
        jdbc.execute("DROP SCHEMA " + SCHEMA + " CASCADE");
    }

    @Test
    void createsCompleteGraphAndEveryReportedGetWorksWithTheAdvertisedAccounts() throws Exception {
        var report = seeder.seed(city);
        assertDemoCounts();
        assertThat(report.accounts()).hasSize(4);
        EMPTY_TABLES.forEach(table -> assertThat(count(table)).as(table + " is not seeded").isZero());
        assertThat(countWhere("notifications", "read_at IS NOT NULL")).isEqualTo(4);
        assertThat(countWhere("notifications", "read_at IS NULL")).isEqualTo(8);
        assertThat(countWhere("notification_preferences", "enabled = false")).isEqualTo(12);
        transactions.executeWithoutResult(status -> {
            for (User user : users.findAll()) {
                assertThat(user.isActivated()).isTrue();
                assertThat(user.isBanned()).isFalse();
                assertThat(user.getTimeZone()).isEqualTo(city.getTimeZoneId());
                assertThat(user.getHomeCity().getId()).isEqualTo(city.getId());
                assertThat(passwordEncoder.matches(PASSWORDS.get(user.getEmail()), user.getPassword())).isTrue();
                assertThat(user.getNotificationPreferencesVersion()).isEqualTo(1);
            }
            for (Event event : entityManager.createQuery("select e from Event e", Event.class).getResultList()) {
                assertThat(event.getAttendeeCount()).isEqualTo(3);
                assertThat(event.getAttendees()).doesNotContain(event.getOwner());
                assertThat(event.getCreateDate()).isBefore(event.getEventStartDate());
                assertThat(event.getOwner().getCreatedAt()).isBefore(event.getCreateDate());
            }
            for (Message message : entityManager.createQuery("select m from Message m", Message.class).getResultList()) {
                assertThat(message.getId()).isPositive();
                assertThat(message.getContent()).doesNotContain("[DEMO");
                assertThat(message.getSenderNameAtCreation()).isEqualTo(message.getSender().getFullName());
                assertThat(encryption.decryptConversationMessage(message.getContent(), message.getEncryptionKeyId()))
                        .hasValueSatisfying(text -> assertThat(text).startsWith("[DEMO message"));
            }
            for (ConversationParticipant participant : entityManager.createQuery("select p from ConversationParticipant p", ConversationParticipant.class).getResultList()) {
                assertThat(participant.getUserNameAtJoin()).isEqualTo(participant.getUser().getFullName());
                assertThat(participant.getJoinedAt()).isAfter(participant.getUser().getCreatedAt());
            }
        });
        Map<String, String> tokens = new HashMap<>();
        for (var account : report.accounts()) {
            var response = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(Map.of("email", account.email(), "password", PASSWORDS.get(account.email())))))
                    .andExpect(status().isOk()).andReturn();
            tokens.put(account.email(), mapper.readTree(response.getResponse().getContentAsString()).get("accessToken").asString());
        }
        for (var resource : report.resources()) {
            if (!resource.protectedRoute()) {
                mvc.perform(get(resource.path())).andExpect(status().isOk());
            } else {
                for (String email : resource.accounts()) {
                    var response = mvc.perform(get(resource.path()).header("Authorization", "Bearer " + tokens.get(email)))
                            .andExpect(status().isOk()).andReturn();
                    if (resource.path().endsWith("/messages")) {
                        assertThat(response.getResponse().getContentAsString()).contains("[DEMO message");
                    } else if (resource.path().endsWith("/data")) {
                        assertThat(response.getResponse().getContentAsByteArray()).isNotEmpty();
                        assertThat(response.getResponse().getContentType()).isIn("application/pdf", "image/png");
                    }
                }
            }
        }
    }

    @Test
    void rerunRetainsIdsContentActivityAndPreferenceVersion() {
        LocalSeedReport first = seeder.seed(city);
        Map<String, List<String>> ids = ids();
        var versions = jdbc.queryForList("SELECT notification_preferences_version FROM users", Long.class);
        Instant future = Instant.parse("2099-01-01T00:00:00Z");
        jdbc.update("UPDATE events SET short_description = 'My edited event'");
        jdbc.update("UPDATE threads SET content = 'My edited thread', last_activity = ?", java.sql.Timestamp.from(future));
        jdbc.update("UPDATE conversations SET last_active_at = ?", java.sql.Timestamp.from(future));

        assertThat(seeder.seed(city)).isEqualTo(first);

        assertThat(ids()).isEqualTo(ids);
        assertDemoCounts();
        assertThat(jdbc.queryForList("SELECT short_description FROM events", String.class)).containsOnly("My edited event");
        assertThat(jdbc.queryForList("SELECT content FROM threads", String.class)).containsOnly("My edited thread");
        assertThat(jdbc.queryForList("SELECT notification_preferences_version FROM users", Long.class)).isEqualTo(versions);
        assertThat(countWhere("threads", "last_activity = '2099-01-01T00:00:00Z'::timestamptz")).isEqualTo(6);
        assertThat(countWhere("conversations", "last_active_at = '2099-01-01T00:00:00Z'::timestamptz")).isEqualTo(3);
    }

    @Test
    void fillsMissingChildrenAndPreservesEditsReadStateAndLeftParticipantsWithCorrectCounters() {
        seeder.seed(city);
        String changedPassword = passwordEncoder.encode("Changed123@");
        UUID userId = users.findByEmail("normal@eventorganizer.com").orElseThrow().getId();
        jdbc.update("UPDATE users SET password = ?, first_name = 'Edited', banned = true, time_zone = 'Europe/London' WHERE id = ?", changedPassword, userId);
        jdbc.update("DELETE FROM user_roles WHERE user_id = ?", userId);
        jdbc.update("UPDATE notifications SET read_at = '2024-01-01'::timestamptz");
        jdbc.update("UPDATE notification_preferences SET enabled = true WHERE user_id = ?", userId);
        UUID directId = jdbc.queryForObject("SELECT id FROM conversations WHERE type = 'DIRECT' LIMIT 1", UUID.class);
        Long lastRead = jdbc.queryForObject("SELECT max(id) FROM messages WHERE conversation_id = ?", Long.class, directId);
        jdbc.update("UPDATE conversation_participant SET left_at = '2024-01-02'::timestamptz, last_read_at = '2024-01-01'::timestamptz, last_read_message_id = ? WHERE conversation_id = ? AND user_id = ?", lastRead, directId, userId);
        UUID threadId = jdbc.queryForObject("SELECT id FROM threads LIMIT 1", UUID.class);
        jdbc.update("DELETE FROM thread_replies WHERE id = (SELECT id FROM thread_replies WHERE thread_id = ? LIMIT 1)", threadId);
        jdbc.update("DELETE FROM messages WHERE id = (SELECT id FROM messages WHERE conversation_id <> ? LIMIT 1)", directId);
        jdbc.update("DELETE FROM files WHERE id = (SELECT id FROM files LIMIT 1)");
        jdbc.update("DELETE FROM notification_preferences WHERE user_id = ? AND resource_type = 'EVENT'", userId);
        jdbc.update("DELETE FROM event_user WHERE ctid = (SELECT ctid FROM event_user LIMIT 1)");
        transactions.executeWithoutResult(status -> {
            Thread thread = entityManager.find(Thread.class, threadId);
            ThreadReply extra = ThreadReply.builder().content("Extra user content").replier(users.findById(userId).orElseThrow())
                    .replyDate(Instant.parse("2099-01-01T00:00:00Z")).lastUpdate(Instant.parse("2099-01-01T00:00:00Z")).build();
            thread.addReplyToThread(extra);
            entityManager.persist(extra);
            Conversation conversation = entityManager.find(Conversation.class, directId);
            var encrypted = encryption.encryptConversationMessage("Extra user message");
            entityManager.persist(new Message(users.findById(userId).orElseThrow(), "Edited User", encrypted.keyId(),
                    encrypted.ciphertext(), Instant.parse("2099-01-01T00:00:00Z"), conversation));
        });

        seeder.seed(city);

        assertThat(count("thread_replies")).isEqualTo(19);
        assertThat(count("messages")).isEqualTo(13);
        assertThat(count("files")).isEqualTo(3);
        assertThat(count("event_user")).isEqualTo(9);
        assertThat(count("notification_preferences")).isEqualTo(12);
        assertThat(count("conversations")).isEqualTo(3);
        User reloaded = users.findById(userId).orElseThrow();
        assertThat(reloaded.getPassword()).isEqualTo(changedPassword);
        assertThat(reloaded.getFirstName()).isEqualTo("Edited");
        assertThat(reloaded.isBanned()).isTrue();
        assertThat(reloaded.getTimeZone()).isEqualTo("Europe/London");
        assertThat(reloaded.getRoles()).isEmpty();
        assertThat(reloaded.getNotificationPreferencesVersion()).isEqualTo(2);
        assertThat(countWhere("notifications", "read_at = '2024-01-01'::timestamptz")).isEqualTo(12);
        assertThat(countWhere("notification_preferences", "enabled = true")).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT last_read_message_id FROM conversation_participant WHERE conversation_id = ? AND user_id = ?", Long.class, directId, userId)).isEqualTo(lastRead);
        assertThat(countWhere("conversation_participant", "left_at IS NOT NULL AND last_read_at IS NOT NULL")).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT reply_count FROM threads WHERE id = ?", Integer.class, threadId)).isEqualTo(4);
        assertThat(countWhere("threads", "last_activity = '2099-01-01T00:00:00Z'::timestamptz")).isEqualTo(1);
        assertThat(countWhere("conversations", "last_active_at = '2099-01-01T00:00:00Z'::timestamptz")).isEqualTo(1);
        assertThat(countWhere("events", "attendee_count = 3")).isEqualTo(3);
    }

    @Test
    void missingEncryptionKeyRollsBackNewGraphWritesInsteadOfDuplicatingMessages() {
        seeder.seed(city);
        jdbc.update("UPDATE messages SET encryption_key_id = 'missing-test-key' WHERE id = (SELECT id FROM messages LIMIT 1)");
        jdbc.update("DELETE FROM files");
        Map<String, List<String>> before = ids();
        assertThatIllegalStateException().isThrownBy(() -> seeder.seed(city))
                .withMessageContaining("Cannot decrypt existing demo conversation");
        assertThat(count("files")).isZero();
        assertThat(ids()).isEqualTo(before);
    }

    @Test
    void initialSeedEncryptionFailureRollsBackEveryDemoEntity() {
        String originalKey = encryptionProperties.getMessageActiveKeyId();
        try {
            encryptionProperties.setMessageActiveKeyId("missing-test-key");
            assertThatIllegalArgumentException().isThrownBy(() -> seeder.seed(city))
                    .withMessageContaining("Message encryption key is not configured");
        } finally {
            encryptionProperties.setMessageActiveKeyId(originalKey);
        }
        TABLES.stream().filter(table -> !table.equals("cities"))
                .forEach(table -> assertThat(count(table)).as(table + " rolled back").isZero());
        EMPTY_TABLES.forEach(table -> assertThat(count(table)).isZero());
        assertThat(count("cities")).isEqualTo(1);
    }

    @Test
    void detectsExistingDemoMessagesBeyondFirstPageAndPreservesAdditionalContent() {
        seeder.seed(city);
        UUID conversationId = jdbc.queryForObject("SELECT id FROM conversations WHERE type = 'GROUP'", UUID.class);
        transactions.executeWithoutResult(status -> {
            Conversation conversation = entityManager.find(Conversation.class, conversationId);
            User sender = users.findByEmail("normal@eventorganizer.com").orElseThrow();
            List<Message> messages = entityManager.createQuery(
                    "select m from Message m where m.conversation = :conversation", Message.class)
                    .setParameter("conversation", conversation).getResultList();
            messages.stream().filter(message -> encryption.decryptConversationMessage(message.getContent(), message.getEncryptionKeyId())
                    .orElseThrow().startsWith("[DEMO message 4]")).forEach(entityManager::remove);
            for (int index = 0; index < 25; index++) {
                var encrypted = encryption.encryptConversationMessage("Extra message " + index);
                entityManager.persist(new Message(sender, sender.getFullName(), encrypted.keyId(), encrypted.ciphertext(),
                        conversation.getCreatedAt().plusSeconds(1000 + index), conversation));
            }
            var encrypted = encryption.encryptConversationMessage("[DEMO message 4] Edited content kept on rerun");
            entityManager.persist(new Message(sender, sender.getFullName(), encrypted.keyId(), encrypted.ciphertext(),
                    conversation.getCreatedAt().plusSeconds(2000), conversation));
        });
        Map<String, List<String>> before = ids();

        seeder.seed(city);

        assertThat(ids()).isEqualTo(before);
        assertThat(count("messages")).isEqualTo(37);
        List<String> contents = transactions.execute(status -> entityManager.createQuery(
                                "select m from Message m where m.conversation.id = :id", Message.class)
                        .setParameter("id", conversationId).getResultList().stream()
                        .map(message -> encryption.decryptConversationMessage(message.getContent(), message.getEncryptionKeyId()).orElseThrow()).toList());
        assertThat(contents).contains("[DEMO message 4] Edited content kept on rerun");
    }

    private void assertDemoCounts() {
        Map<String, Integer> expected = Map.ofEntries(Map.entry("cities", 1), Map.entry("users", 4), Map.entry("tags", 4),
                Map.entry("events", 3), Map.entry("event_user", 9), Map.entry("event_tag", 6), Map.entry("threads", 6),
                Map.entry("thread_replies", 18), Map.entry("files", 3), Map.entry("conversations", 3),
                Map.entry("conversation_participant", 8), Map.entry("direct_conversation_pair", 2),
                Map.entry("messages", 12), Map.entry("notifications", 12), Map.entry("notification_preferences", 12));
        expected.forEach((table, amount) -> assertThat(count(table)).as(table).isEqualTo(amount));
        assertThat(countWhere("events", "max_attendees IS NULL")).isEqualTo(1);
        assertThat(countWhere("events", "max_attendees = 3 AND attendee_count = 3")).isEqualTo(1);
    }

    private int count(String table) { return countWhere(table, "true"); }
    private int countWhere(String table, String predicate) {
        return jdbc.queryForObject("SELECT count(*) FROM " + SCHEMA + "." + table + " WHERE " + predicate, Integer.class);
    }
    private Map<String, List<String>> ids() {
        Map<String, List<String>> result = new HashMap<>();
        for (String table : TABLES) {
            if (!Set.of("event_user", "event_tag").contains(table)) {
                result.put(table, jdbc.queryForList("SELECT id::text FROM " + SCHEMA + "." + table + " ORDER BY id::text", String.class));
            }
        }
        return result;
    }
}
