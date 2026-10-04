package com.mazurek.eventOrganizer.config.seed;

import com.mazurek.eventOrganizer.config.DataInitializer;
import com.mazurek.eventOrganizer.config.properties.SeedProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.server.Ssl;
import org.springframework.boot.web.server.autoconfigure.ServerProperties;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.boot.web.server.context.WebServerInitializedEvent;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Profile;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class LocalSeedReportPrinterUnitTest {
    private final SeedProperties properties = new SeedProperties();
    private final ServerProperties serverProperties = new ServerProperties();
    private final LocalSeedReportPrinter printer = new LocalSeedReportPrinter(properties, serverProperties);
    private final LocalSeedReport report = new LocalSeedReport(
            List.of(new LocalSeedReport.Account(UUID.randomUUID(), "normal@eventorganizer.com", List.of("ROLE_USER"))),
            List.of(new LocalSeedReport.Resource("Event", "/api/v1/events/id", List.of()),
                    new LocalSeedReport.Resource("Messages", "/api/v1/conversations/id/messages", List.of("normal@eventorganizer.com"))));

    @Test
    void customRootIsNormalizedAndAccessIsExplainedWithoutCredentials() {
        properties.setApiBaseUrl("https://demo.example/backend///");
        String output = String.join("\n", printer.lines(report));
        assertThat(output).contains("GET https://demo.example/backend/api/v1/events/id | public")
                .contains("GET https://demo.example/backend/api/v1/conversations/id/messages | JWT: normal@eventorganizer.com")
                .contains("POST https://demo.example/backend/api/v1/auth/login")
                .doesNotContain("Normal123@", "Admin123@", "Participant123@", "Moderator123@", "accessToken", "refreshToken");
    }

    @Test
    void usesActualServerPortSslAndEncodedContextPathIgnoringManagementServer() {
        serverProperties.setPort(0);
        serverProperties.setSsl(new Ssl());
        serverProperties.getServlet().setContextPath("/demo app");
        var context = mock(WebServerApplicationContext.class);
        var server = mock(WebServer.class);
        when(server.getPort()).thenReturn(12345);
        var event = mock(WebServerInitializedEvent.class);
        when(event.getApplicationContext()).thenReturn(context);
        when(event.getWebServer()).thenReturn(server);
        printer.onApplicationEvent(event);
        when(context.getServerNamespace()).thenReturn("management");
        when(server.getPort()).thenReturn(9999);
        printer.onApplicationEvent(event);
        assertThat(String.join("\n", printer.lines(report))).contains("https://localhost:12345/demo%20app/api/v1/events/id");
    }

    @ParameterizedTest
    @ValueSource(strings = {"ftp://example.com", "https://user:secret@example.com", "https://example.com?token=secret",
            "https://example.com#secret", "not a url", "https://example.com:65536", "https://example.com:0"})
    void rejectsUnsafeBaseUrlWithoutEchoingItsValue(String value) {
        properties.setApiBaseUrl(value);
        assertThatIllegalArgumentException().isThrownBy(printer::validateBaseUrl)
                .withMessageContaining("APP_SEED_API_BASE_URL").withMessageNotContaining("secret");
    }

    @Test
    void defaultsToLocalhost8080() {
        assertThat(String.join("\n", printer.lines(report))).contains("http://localhost:8080/api/v1/events/id")
                .doesNotContain("frontend.example");
    }

    @Test
    void explicitlyDisabledSslProducesHttpDespiteConfiguredSslBundle() {
        Ssl ssl = new Ssl();
        ssl.setBundle("demo-bundle");
        ssl.setEnabled(false);
        serverProperties.setSsl(ssl);
        serverProperties.setPort(9090);
        assertThat(String.join("\n", printer.lines(report))).contains("http://localhost:9090/api/v1/events/id");
    }

    @Test
    void seedingComponentsAreLocalOnlyAndReportListsAreImmutable() {
        for (Class<?> type : List.of(DataInitializer.class, LocalDemoSeeder.class, LocalSeedReportPrinter.class)) {
            assertThat(type.getAnnotation(Profile.class).value()).containsExactly("local");
        }
        assertThatThrownBy(() -> report.accounts().clear()).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> report.resources().clear()).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> report.accounts().getFirst().roles().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"test", "production"})
    void componentsAreNotRegisteredOutsideLocalProfile(String profile) {
        new ApplicationContextRunner().withUserConfiguration(DataInitializer.class, LocalDemoSeeder.class, LocalSeedReportPrinter.class)
                .withInitializer(context -> context.getEnvironment().setActiveProfiles(profile))
                .run(context -> {
                    assertThat(context).doesNotHaveBean(DataInitializer.class).doesNotHaveBean(LocalDemoSeeder.class)
                            .doesNotHaveBean(LocalSeedReportPrinter.class);
                });
    }

    @Test
    void conversationWithoutActiveAccountsIsNotAdvertisedAsPublic() {
        var inaccessible = new LocalSeedReport(List.of(), List.of(
                new LocalSeedReport.Resource("Conversation", "/api/v1/conversations/id", List.of(), true)));
        assertThat(String.join("\n", printer.lines(inaccessible))).contains("JWT: no active demo account").doesNotContain("| public");
    }
}
