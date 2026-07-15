package com.oneil.wellness.walkplanner.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.filter.CorsFilter;

import jakarta.servlet.ServletException;

class CorsConfigTest {

    @Test
    void allowsConfiguredFrontendOriginForApiRequests() throws ServletException, IOException {
        CorsConfig corsConfig = new CorsConfig();
        CorsFilter filter = corsConfig.corsFilter("http://localhost:3000,http://127.0.0.1:3000");
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/health/status");
        request.addHeader("Origin", "http://localhost:3000");
        request.addHeader("Access-Control-Request-Method", "GET");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getHeader("Access-Control-Allow-Origin")).isEqualTo("http://localhost:3000");
        assertThat(response.getHeader("Access-Control-Allow-Methods")).contains("GET");
    }
}
