package com.sensorbite.evacroute.domain.model;

import java.util.List;
import java.util.stream.IntStream;

public class RoadSegment {
    private static final int MIN_SEGMENT_COORDINATES = 2;

    private final String id;
    private final List<Coordinate> coordinates;
    private final double lengthMeters;
    private final boolean oneway;
    private final boolean hazardous;

    public RoadSegment(String id, List<Coordinate> coordinates, boolean oneway) {
        this(id, coordinates, oneway, false);
    }

    public RoadSegment(String id, List<Coordinate> coordinates, boolean oneway, boolean hazardous) {
        if (coordinates == null || coordinates.size() < MIN_SEGMENT_COORDINATES) {
            throw new IllegalArgumentException("Segment must have at least " + MIN_SEGMENT_COORDINATES + " points");
        }
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Segment ID cannot be null or blank");
        }
        this.id = id;
        this.coordinates = List.copyOf(coordinates);
        this.lengthMeters = calculateLength();
        this.oneway = oneway;
        this.hazardous = hazardous;
    }

    private double calculateLength() {
        return IntStream.range(0, coordinates.size() - 1)
                .mapToDouble(i -> coordinates.get(i).distanceTo(coordinates.get(i + 1)))
                .sum();
    }

    public RoadSegment withHazardous(boolean hazardous) {
        if (this.hazardous == hazardous) {
            return this;
        }
        return new RoadSegment(id, coordinates, oneway, hazardous);
    }

    public String getId() {
        return id;
    }

    public List<Coordinate> getCoordinates() {
        return coordinates;
    }

    public double getLengthMeters() {
        return lengthMeters;
    }

    public boolean isOneway() {
        return oneway;
    }

    public boolean isHazardous() {
        return hazardous;
    }
}
