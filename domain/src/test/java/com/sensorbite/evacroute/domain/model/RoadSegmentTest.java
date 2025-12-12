package com.sensorbite.evacroute.domain.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RoadSegmentTest {

    @Test
    void shouldCreateValidSegment() {
        List<Coordinate> coords = List.of(
                new Coordinate(52.0, 21.0),
                new Coordinate(52.1, 21.1)
        );

        RoadSegment segment = new RoadSegment("seg1", coords, false);

        assertThat(segment.getId()).isEqualTo("seg1");
        assertThat(segment.getCoordinates()).hasSize(2);
        assertThat(segment.isOneway()).isFalse();
        assertThat(segment.isHazardous()).isFalse();
        assertThat(segment.getLengthMeters()).isGreaterThan(0);
    }

    @Test
    void shouldRejectSegmentWithLessThanTwoPoints() {
        List<Coordinate> coords = List.of(new Coordinate(52.0, 21.0));

        assertThatThrownBy(() -> new RoadSegment("seg1", coords, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least 2 points");
    }

    @Test
    void shouldMarkSegmentAsHazardous() {
        List<Coordinate> coords = List.of(
                new Coordinate(52.0, 21.0),
                new Coordinate(52.1, 21.1)
        );

        RoadSegment segment = new RoadSegment("seg1", coords, false);
        assertThat(segment.isHazardous()).isFalse();

        RoadSegment hazardousSegment = segment.withHazardous(true);
        assertThat(hazardousSegment.isHazardous()).isTrue();
        assertThat(segment.isHazardous()).isFalse(); // Original unchanged
    }

    @Test
    void shouldSupportOnewaySegments() {
        List<Coordinate> coords = List.of(
                new Coordinate(52.0, 21.0),
                new Coordinate(52.1, 21.1)
        );

        RoadSegment segment = new RoadSegment("seg1", coords, true);
        assertThat(segment.isOneway()).isTrue();
    }
}
