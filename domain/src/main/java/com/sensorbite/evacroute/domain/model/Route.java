package com.sensorbite.evacroute.domain.model;

import java.util.List;

public class Route {
    private final List<RoadSegment> segments;
    private final RouteMetadata metadata;

    public Route(List<RoadSegment> segments, RouteMetadata metadata) {
        if (segments == null) {
            throw new IllegalArgumentException("Route segments cannot be null");
        }
        if (metadata == null) {
            throw new IllegalArgumentException("Metadata cannot be null");
        }
        this.segments = List.copyOf(segments);
        this.metadata = metadata;
    }

    public List<RoadSegment> getSegments() {
        return segments;
    }

    public RouteMetadata getMetadata() {
        return metadata;
    }

    public List<Coordinate> getCoordinates() {
        return segments.stream()
                .flatMap(segment -> segment.getCoordinates().stream())
                .toList();
    }
}
