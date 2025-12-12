package com.sensorbite.evacroute.domain.model;

import java.time.Instant;

public record RouteMetadata(
    double distanceMeters,
    long computationTimeMs,
    int hazardousSegmentsAvoided,
    double safetyScore,
    Instant timestamp,
    boolean allPathsHazardous
) {
    public RouteMetadata {
        if (safetyScore < 0.0 || safetyScore > 1.0) {
            throw new IllegalArgumentException("Safety score must be [0.0, 1.0], got: " + safetyScore);
        }
        if (distanceMeters < 0) {
            throw new IllegalArgumentException("Distance cannot be negative");
        }
        if (computationTimeMs < 0) {
            throw new IllegalArgumentException("Computation time cannot be negative");
        }
    }
}
