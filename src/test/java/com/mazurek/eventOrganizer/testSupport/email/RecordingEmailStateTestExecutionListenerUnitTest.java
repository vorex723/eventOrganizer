package com.mazurek.eventOrganizer.testSupport.email;

import com.mazurek.eventOrganizer.notification.service.RecordingEmailService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestContext;

import java.util.Map;

import static org.mockito.Mockito.*;

@DisplayName("RecordingEmailStateTestExecutionListener unit tests:")
class RecordingEmailStateTestExecutionListenerUnitTest {

    private final RecordingEmailStateTestExecutionListener listener =
            new RecordingEmailStateTestExecutionListener();

    @Test
    void whenContextIsUnavailableShouldNotLoadItForSetupOrCleanup() {
        TestContext testContext = mock(TestContext.class);

        listener.beforeTestMethod(testContext);
        listener.afterTestMethod(testContext);

        verify(testContext, times(2)).hasApplicationContext();
        verify(testContext, never()).getApplicationContext();
    }

    @Test
    void whenContextHasNoRecordingServiceShouldLeaveFocusedContextsUntouched() {
        TestContext testContext = mock(TestContext.class);
        ApplicationContext context = mock(ApplicationContext.class);
        when(testContext.hasApplicationContext()).thenReturn(true);
        when(testContext.getApplicationContext()).thenReturn(context);
        when(context.getBeansOfType(RecordingEmailService.class, false, false)).thenReturn(Map.of());

        listener.beforeTestMethod(testContext);
        listener.afterTestMethod(testContext);

        verify(context, times(2)).getBeansOfType(RecordingEmailService.class, false, false);
        verifyNoMoreInteractions(context);
    }

    @ParameterizedTest(name = "Before method: {0}")
    @ValueSource(booleans = {true, false})
    void whenMethodBoundaryIsReachedShouldResetEveryRecordingService(boolean beforeMethod) {
        TestContext testContext = mock(TestContext.class);
        ApplicationContext context = mock(ApplicationContext.class);
        RecordingEmailService firstService = mock(RecordingEmailService.class);
        RecordingEmailService secondService = mock(RecordingEmailService.class);
        when(testContext.hasApplicationContext()).thenReturn(true);
        when(testContext.getApplicationContext()).thenReturn(context);
        when(context.getBeansOfType(RecordingEmailService.class, false, false))
                .thenReturn(Map.of("first", firstService, "second", secondService));

        if (beforeMethod) {
            listener.beforeTestMethod(testContext);
        } else {
            listener.afterTestMethod(testContext);
        }

        verify(firstService).reset();
        verify(secondService).reset();
        verifyNoMoreInteractions(firstService, secondService);
    }
}
