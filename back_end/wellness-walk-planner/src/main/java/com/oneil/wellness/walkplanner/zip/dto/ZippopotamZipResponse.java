package com.oneil.wellness.walkplanner.zip.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ZippopotamZipResponse(
        @JsonProperty("post code") String postCode,
        String country,
        @JsonProperty("country abbreviation") String countryAbbreviation,
        List<ZippopotamPlaceResponse> places) {
}
