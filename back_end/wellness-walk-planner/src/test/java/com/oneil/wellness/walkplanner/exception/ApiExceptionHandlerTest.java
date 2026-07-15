package com.oneil.wellness.walkplanner.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class ApiExceptionHandlerTest {

    @Test
    void responseStatusExceptionReturnsCompactApplicationError() {
        ApiExceptionHandler handler = new ApiExceptionHandler();

        var response = handler.handleResponseStatusException(
                new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter a valid 5-digit ZIP code"));

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(400);
        assertThat(response.getBody().message()).isEqualTo("Enter a valid 5-digit ZIP code");
    }
}
