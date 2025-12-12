package com.sensorbite.evacroute.application.dto;

public record PropertiesDto(
    double distanceMeters,
    long computationTimeMs,
    int hazardousSegmentsAvoided,
    double safetyScore,
    String timestamp,
    boolean allPathsHazardous
) {}
