package com.mazurek.eventOrganizer.testData.builders;

import com.mazurek.eventOrganizer.config.seed.LocalSeedReport;
import com.mazurek.eventOrganizer.config.seed.LocalSeedReport.Account;
import com.mazurek.eventOrganizer.config.seed.LocalSeedReport.Resource;
import java.util.ArrayList;
import java.util.List;



/** Constructs data only; explicit overrides are passed through without repair. */
public class LocalSeedReportTestBuilder {
    private List<Account> accounts;
    private boolean accountsSet;
    private List<Resource> resources;
    private boolean resourcesSet;

    public LocalSeedReportTestBuilder accounts(List<Account> accounts) {
        this.accounts = accounts == null ? null : new ArrayList<>(accounts);
        this.accountsSet = true;
        return this;
    }

    public LocalSeedReportTestBuilder resources(List<Resource> resources) {
        this.resources = resources == null ? null : new ArrayList<>(resources);
        this.resourcesSet = true;
        return this;
    }


    public LocalSeedReport build() {
        return new LocalSeedReport(
                accountsSet ? accounts == null ? null : new ArrayList<>(accounts) : List.of(new LocalSeedReportAccountTestBuilder().build()),
                resourcesSet ? resources == null ? null : new ArrayList<>(resources) : List.of(new LocalSeedReportResourceTestBuilder().build())
        );
    }
}
