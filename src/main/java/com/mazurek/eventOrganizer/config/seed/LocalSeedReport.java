package com.mazurek.eventOrganizer.config.seed;

import java.util.List;
import java.util.UUID;

/** Detached, immutable values only: safe to print after the seed transaction commits. */
public record LocalSeedReport(List<Account> accounts, List<Resource> resources) {
    public LocalSeedReport {
        accounts = List.copyOf(accounts);
        resources = List.copyOf(resources);
    }

    public record Account(UUID id, String email, List<String> roles) {
        public Account {
            roles = List.copyOf(roles);
        }
    }

    public record Resource(String label, String path, List<String> accounts, boolean protectedRoute) {
        public Resource(String label, String path, List<String> accounts) {
            this(label, path, accounts, !accounts.isEmpty());
        }

        public Resource {
            accounts = List.copyOf(accounts);
        }
    }
}
