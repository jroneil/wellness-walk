package com.oneil.wellness.walkplanner.dto;

public record BackendStatusResponse(
        String applicationName,
        String status,
        String backendTimestamp,
        String developmentStage) {
}
