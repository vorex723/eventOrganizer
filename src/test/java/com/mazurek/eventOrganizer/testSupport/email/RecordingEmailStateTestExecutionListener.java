package com.mazurek.eventOrganizer.testSupport.email;

import com.mazurek.eventOrganizer.notification.service.RecordingEmailService;
import com.mazurek.eventOrganizer.testSupport.concurrency.TestWorkers;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;

/**
 * Owns recording cleanup independently of test database transactions/teardown.
 * Runs before user setup and after user teardown, including failed methods.
 * The inherited lowest precedence makes afterTestMethod run before context eviction.
 */
public class RecordingEmailStateTestExecutionListener extends AbstractTestExecutionListener {

    @Override
    public void beforeTestMethod(TestContext testContext) {
        resetRecordings(testContext);
    }

    @Override
    public void afterTestMethod(TestContext testContext) {
        resetRecordings(testContext);
    }

    private void resetRecordings(TestContext testContext) {
        TestWorkers.requireStopped();
        // A failed startup must not cause a new context load during cleanup.
        if (!testContext.hasApplicationContext()) {
            return;
        }
        testContext.getApplicationContext()
                .getBeansOfType(RecordingEmailService.class, false, false)
                .values().forEach(RecordingEmailService::reset);
    }
}
