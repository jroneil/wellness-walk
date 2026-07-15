package com.oneil.wellness.walkplanner.zip.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.http.HttpTimeoutException;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oneil.wellness.walkplanner.config.ZipLookupProperties;
import com.oneil.wellness.walkplanner.zip.service.ZipCodeNotFoundException;
import com.oneil.wellness.walkplanner.zip.service.ZipLookupException;

class ZipLookupClientTest {

    @Test
    void returnsCoordinatesForSuccessfulZipLookup() {
        ZipLookupClient client = clientReturning(200, """
                {
                  "post code": "01830",
                  "country": "United States",
                  "country abbreviation": "US",
                  "places": [
                    {
                      "place name": "Haverhill",
                      "longitude": "-71.0773",
                      "state": "Massachusetts",
                      "state abbreviation": "MA",
                      "latitude": "42.7762"
                    }
                  ]
                }
                """);

        var coordinates = client.lookup("01830");

        assertThat(coordinates.latitude()).isEqualByComparingTo("42.7762");
        assertThat(coordinates.longitude()).isEqualByComparingTo("-71.0773");
    }

    @Test
    void throwsNotFoundForProviderNotFoundResponse() {
        ZipLookupClient client = clientReturning(404, "{}");

        assertThatThrownBy(() -> client.lookup("00000"))
                .isInstanceOf(ZipCodeNotFoundException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void throwsLookupExceptionForProviderError() {
        ZipLookupClient client = clientReturning(500, "{}");

        assertThatThrownBy(() -> client.lookup("01830"))
                .isInstanceOf(ZipLookupException.class)
                .hasMessageContaining("error response");
    }

    @Test
    void throwsLookupExceptionForProviderTimeout() {
        ZipLookupClient client = new ZipLookupClient(properties(), new ObjectMapper(), request -> {
            throw new HttpTimeoutException("timed out");
        });

        assertThatThrownBy(() -> client.lookup("01830"))
                .isInstanceOf(ZipLookupException.class)
                .hasMessageContaining("timed out");
    }

    private ZipLookupClient clientReturning(int statusCode, String responseBody) {
        return new ZipLookupClient(properties(), new ObjectMapper(),
                request -> new ZipLookupHttpResponse(statusCode, responseBody));
    }

    private ZipLookupProperties properties() {
        ZipLookupProperties properties = new ZipLookupProperties();
        properties.setBaseUrl("https://example.test");
        properties.setConnectionTimeoutMs(1000);
        properties.setResponseTimeoutMs(1000);
        return properties;
    }
}
