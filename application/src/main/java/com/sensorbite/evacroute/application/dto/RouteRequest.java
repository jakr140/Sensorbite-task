package com.sensorbite.evacroute.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record RouteRequest(
    @NotBlank(message = "Start coordinate is required")
    @Pattern(regexp = "^-?\\d+(\\.\\d+)?,-?\\d+(\\.\\d+)?$",
             message = "Invalid coordinate format. Expected: 'lat,lon' (e.g., '52.2297,21.0122')")
    String start,

    @NotBlank(message = "End coordinate is required")
    @Pattern(regexp = "^-?\\d+(\\.\\d+)?,-?\\d+(\\.\\d+)?$",
             message = "Invalid coordinate format. Expected: 'lat,lon' (e.g., '52.2400,21.0250')")
    String end
) {}
