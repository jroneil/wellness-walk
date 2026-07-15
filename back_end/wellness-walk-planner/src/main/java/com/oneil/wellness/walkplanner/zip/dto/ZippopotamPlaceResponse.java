package com.oneil.wellness.walkplanner.zip.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ZippopotamPlaceResponse(
        @JsonProperty("place name") String placeName,
        String longitude,
        String state,
        @JsonProperty("state abbreviation") String stateAbbreviation,
        String latitude) {
}
