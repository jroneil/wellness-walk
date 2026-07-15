package com.oneil.wellness.walkplanner.dto;

public record ApplicationErrorResponse(
        int status,
        String error,
        String message) {
}
