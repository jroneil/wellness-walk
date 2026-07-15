package com.oneil.wellness.walkplanner.controller;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class HealthControllerTest {

    @Test
    void statusReturnsExpectedPayloadShape() {
        HealthController controller = new HealthController("walk-planer", "phase-1");

        var response = controller.status();

        assertThat(response.applicationName()).isEqualTo("walk-planer");
        assertThat(response.status()).isEqualTo("UP");
        assertThat(response.backendTimestamp()).isNotBlank();
        assertThat(response.developmentStage()).isEqualTo("phase-1");
    }
}
