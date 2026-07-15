package com.oneil.wellness.walkplanner.zip.client;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oneil.wellness.walkplanner.config.ZipLookupProperties;
import com.oneil.wellness.walkplanner.zip.dto.ZippopotamPlaceResponse;
import com.oneil.wellness.walkplanner.zip.dto.ZippopotamZipResponse;
import com.oneil.wellness.walkplanner.zip.service.ZipCodeNotFoundException;
import com.oneil.wellness.walkplanner.zip.service.ZipCoordinates;
import com.oneil.wellness.walkplanner.zip.service.ZipLookupException;

@Component
public class ZipLookupClient {

    private final ZipLookupProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final ZipLookupTransport transport;

    @Autowired
    public ZipLookupClient(ZipLookupProperties properties) {
        this(properties, new ObjectMapper(), HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(properties.getConnectionTimeoutMs()))
                .build(), null);
    }

    ZipLookupClient(ZipLookupProperties properties, ObjectMapper objectMapper, ZipLookupTransport transport) {
        this(properties, objectMapper, null, transport);
    }

    private ZipLookupClient(ZipLookupProperties properties, ObjectMapper objectMapper, HttpClient httpClient, ZipLookupTransport transport) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
        this.transport = transport;
    }

    public ZipCoordinates lookup(String zipCode) {
        URI uri = URI.create(properties.getBaseUrl() + "/us/" + zipCode);
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofMillis(properties.getResponseTimeoutMs()))
                .header("Accept", "application/json")
                .build();

        try {
            ZipLookupHttpResponse response = send(request);
            if (response.statusCode() == 404) {
                throw new ZipCodeNotFoundException("ZIP code not found");
            }
            if (response.statusCode() >= 400) {
                throw new ZipLookupException("ZIP lookup provider returned an error response");
            }
            return parse(response.body());
        } catch (JsonProcessingException | NumberFormatException ex) {
            throw new ZipLookupException("Malformed ZIP lookup provider response", ex);
        } catch (java.net.http.HttpTimeoutException ex) {
            throw new ZipLookupException("ZIP lookup provider timed out", ex);
        } catch (java.io.IOException ex) {
            throw new ZipLookupException("ZIP lookup provider unavailable", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ZipLookupException("ZIP lookup provider request interrupted", ex);
        }
    }

    private ZipLookupHttpResponse send(HttpRequest request) throws java.io.IOException, InterruptedException {
        if (transport != null) {
            return transport.send(request);
        }
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return new ZipLookupHttpResponse(response.statusCode(), response.body());
    }

    private ZipCoordinates parse(String body) throws JsonProcessingException {
        ZippopotamZipResponse zipResponse = objectMapper.readValue(body, ZippopotamZipResponse.class);
        if (zipResponse.places() == null || zipResponse.places().isEmpty()) {
            throw new ZipCodeNotFoundException("ZIP code not found");
        }

        ZippopotamPlaceResponse place = zipResponse.places().getFirst();
        return new ZipCoordinates(new BigDecimal(place.latitude()), new BigDecimal(place.longitude()));
    }
}
