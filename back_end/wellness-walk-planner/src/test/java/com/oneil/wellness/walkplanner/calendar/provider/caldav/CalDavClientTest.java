package com.oneil.wellness.walkplanner.calendar.provider.caldav;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class CalDavClientTest {
    @Test
    void rejectsResponsesAboveTheConfiguredByteLimit() {
        var configuration = new CalDavConfiguration();
        configuration.setMaximumResponseBytes(1024);
        var client = new CalDavClient(configuration);

        client.validateResponseSize("a".repeat(1024));
        var exception = assertThrows(CalDavException.class,
                () -> client.validateResponseSize("é".repeat(513)));

        assertEquals("RESPONSE_TOO_LARGE", exception.getCode());
    }
}
