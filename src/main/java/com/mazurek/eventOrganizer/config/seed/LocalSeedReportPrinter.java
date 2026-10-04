package com.mazurek.eventOrganizer.config.seed;

import com.mazurek.eventOrganizer.config.properties.SeedProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.server.context.WebServerInitializedEvent;
import org.springframework.boot.web.server.autoconfigure.ServerProperties;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@Component
@Profile("local")
@RequiredArgsConstructor
@Slf4j
public class LocalSeedReportPrinter implements ApplicationListener<WebServerInitializedEvent> {
    private final SeedProperties properties;
    private final ServerProperties serverProperties;
    private Integer serverPort;

    @Override
    public void onApplicationEvent(WebServerInitializedEvent event) {
        // Ignore a separate management server.
        if (event.getApplicationContext().getServerNamespace() == null) {
            serverPort = event.getWebServer().getPort();
        }
    }

    public void validateBaseUrl() {
        baseUrl();
    }

    public void print(LocalSeedReport report) {
        log.info("{}", String.join(System.lineSeparator(), lines(report)));
    }

    public List<String> lines(LocalSeedReport report) {
        String base = baseUrl();
        List<String> lines = new ArrayList<>();
        lines.add("Local demo data ready (existing accounts keep their credentials and settings).");
        report.accounts().forEach(account -> lines.add("Account " + account.email() + " | "
                + account.id() + " | " + String.join(", ", account.roles())));
        lines.add("Protected GET links require Bearer JWT obtained through POST " + base + "/api/v1/auth/login");
        for (var resource : report.resources()) {
            String access = !resource.protectedRoute() ? "public" : "JWT: "
                    + (resource.accounts().isEmpty() ? "no active demo account" : String.join(", ", resource.accounts()));
            lines.add(resource.label() + " | GET " + base + resource.path() + " | " + access);
        }
        return List.copyOf(lines);
    }

    private String baseUrl() {
        String configured = properties.getApiBaseUrl();
        if (configured != null && !configured.isBlank()) {
            URI uri;
            try {
                uri = URI.create(configured.strip());
            } catch (IllegalArgumentException exception) {
                throw invalidBaseUrl();
            }
            if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    || uri.getHost() == null || uri.getUserInfo() != null || uri.getQuery() != null
                    || uri.getFragment() != null || uri.getPort() > 65535 || uri.getPort() == 0) {
                throw invalidBaseUrl();
            }
            return uri.toASCIIString().replaceAll("/+$", "");
        }
        int port = serverPort != null ? serverPort : (serverProperties.getPort() == null ? 8080 : serverProperties.getPort());
        if (port <= 0) {
            throw new IllegalStateException("Demo links require a running web server or APP_SEED_API_BASE_URL.");
        }
        // SSL bundles also enable HTTPS without an explicit server.ssl.enabled property.
        String scheme = serverProperties.getSsl() != null && serverProperties.getSsl().isEnabled() ? "https" : "http";
        String contextPath = serverProperties.getServlet().getContextPath();
        if (contextPath == null) {
            contextPath = "";
        }
        return UriComponentsBuilder.newInstance().scheme(scheme).host("localhost").port(port)
                .path(contextPath).build().encode().toUriString().replaceAll("/+$", "");
    }

    private IllegalArgumentException invalidBaseUrl() {
        // Do not echo a potentially secret-bearing invalid value.
        return new IllegalArgumentException("APP_SEED_API_BASE_URL must be an HTTP(S) application root without credentials, query or fragment.");
    }
}
