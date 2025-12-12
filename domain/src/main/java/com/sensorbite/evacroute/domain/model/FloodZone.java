package com.sensorbite.evacroute.domain.model;

import java.time.Instant;
import java.util.List;

public record FloodZone(
    String id,
    List<List<Coordinate>> polygonRings,
    Instant validFrom,
    Instant validUntil
) {
    public FloodZone {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("FloodZone ID cannot be null or blank");
        }
        if (polygonRings == null || polygonRings.isEmpty()) {
            throw new IllegalArgumentException("FloodZone must have at least one polygon ring");
        }

        for (int i = 0; i < polygonRings.size(); i++) {
            List<Coordinate> ring = polygonRings.get(i);
            if (ring == null || ring.size() < 3) {
                throw new IllegalArgumentException(
                    "Polygon ring " + i + " must have at least 3 points, got: " +
                    (ring == null ? "null" : ring.size())
                );
            }
        }

        polygonRings = List.copyOf(polygonRings.stream()
            .map(List::copyOf)
            .toList());
    }

    public boolean isValidAt(Instant timestamp) {
        if (validFrom == null && validUntil == null) {
            return true;
        }
        boolean afterStart = validFrom == null || !timestamp.isBefore(validFrom);
        boolean beforeEnd = validUntil == null || !timestamp.isAfter(validUntil);
        return afterStart && beforeEnd;
    }
}
