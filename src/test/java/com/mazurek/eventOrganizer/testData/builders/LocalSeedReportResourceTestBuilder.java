package com.mazurek.eventOrganizer.testData.builders;

import com.mazurek.eventOrganizer.config.seed.LocalSeedReport.Resource;
import java.util.ArrayList;
import java.util.List;

import static com.mazurek.eventOrganizer.testData.TestConstants.SeedReportConstants;

/** Constructs data only; explicit overrides are passed through without repair. */
public class LocalSeedReportResourceTestBuilder {
    private String label = SeedReportConstants.LABEL;
    private String path = SeedReportConstants.PATH;
    private List<String> accounts = List.of();
    private boolean protectedRoute = SeedReportConstants.PROTECTED_ROUTE;

    public LocalSeedReportResourceTestBuilder label(String label) {
        this.label = label;
        return this;
    }

    public LocalSeedReportResourceTestBuilder path(String path) {
        this.path = path;
        return this;
    }

    public LocalSeedReportResourceTestBuilder accounts(List<String> accounts) {
        this.accounts = accounts == null ? null : new ArrayList<>(accounts);
        return this;
    }

    public LocalSeedReportResourceTestBuilder protectedRoute(boolean protectedRoute) {
        this.protectedRoute = protectedRoute;
        return this;
    }


    public Resource build() {
        return new Resource(
                label,
                path,
                accounts == null ? null : new ArrayList<>(accounts),
                protectedRoute
        );
    }
}
