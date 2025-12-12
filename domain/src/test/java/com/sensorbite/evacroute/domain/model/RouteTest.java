package com.sensorbite.evacroute.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Route")
class RouteTest {

    @Nested
    @DisplayName("Validation tests")
    class ValidationTests {

        @Test
        @DisplayName("should create valid route")
        void shouldCreateValidRoute() {
            List<RoadSegment> segments = List.of(
                    new RoadSegment("seg1", List.of(
                            new Coordinate(52.0, 21.0),
                            new Coordinate(52.1, 21.1)
                    ), false)
            );
            RouteMetadata metadata = new RouteMetadata(100.0, 50L, 0, 1.0, Instant.now(), false);

            Route route = new Route(segments, metadata);

            assertThat(route.getSegments()).hasSize(1);
            assertThat(route.getMetadata()).isNotNull();
        }

        @Test
        @DisplayName("should reject null segments")
        void shouldRejectNullSegments() {
            RouteMetadata metadata = new RouteMetadata(100.0, 50L, 0, 1.0, Instant.now(), false);

            assertThatThrownBy(() -> new Route(null, metadata))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Route segments cannot be null");
        }

        @Test
        @DisplayName("should allow empty segments for empty routes")
        void shouldAllowEmptySegmentsForEmptyRoutes() {
            RouteMetadata metadata = new RouteMetadata(0.0, 50L, 0, 1.0, Instant.now(), false);

            Route route = new Route(List.of(), metadata);

            assertThat(route.getSegments()).isEmpty();
            assertThat(route.getCoordinates()).isEmpty();
        }

        @Test
        @DisplayName("should reject null metadata")
        void shouldRejectNullMetadata() {
            List<RoadSegment> segments = List.of(
                    new RoadSegment("seg1", List.of(
                            new Coordinate(52.0, 21.0),
                            new Coordinate(52.1, 21.1)
                    ), false)
            );

            assertThatThrownBy(() -> new Route(segments, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Metadata cannot be null");
        }
    }

    @Nested
    @DisplayName("Coordinate extraction")
    class CoordinateExtractionTests {

        @Test
        @DisplayName("should extract coordinates from single segment")
        void shouldExtractCoordinatesFromSingleSegment() {
            List<RoadSegment> segments = List.of(
                    new RoadSegment("seg1", List.of(
                            new Coordinate(52.0, 21.0),
                            new Coordinate(52.1, 21.1)
                    ), false)
            );
            RouteMetadata metadata = new RouteMetadata(100.0, 50L, 0, 1.0, Instant.now(), false);
            Route route = new Route(segments, metadata);

            List<Coordinate> coords = route.getCoordinates();

            assertThat(coords).hasSize(2);
            assertThat(coords.get(0)).isEqualTo(new Coordinate(52.0, 21.0));
            assertThat(coords.get(1)).isEqualTo(new Coordinate(52.1, 21.1));
        }

        @Test
        @DisplayName("should extract coordinates from multiple segments")
        void shouldExtractCoordinatesFromMultipleSegments() {
            List<RoadSegment> segments = List.of(
                    new RoadSegment("seg1", List.of(
                            new Coordinate(52.0, 21.0),
                            new Coordinate(52.1, 21.1)
                    ), false),
                    new RoadSegment("seg2", List.of(
                            new Coordinate(52.1, 21.1),
                            new Coordinate(52.2, 21.2)
                    ), false)
            );
            RouteMetadata metadata = new RouteMetadata(200.0, 50L, 0, 1.0, Instant.now(), false);
            Route route = new Route(segments, metadata);

            List<Coordinate> coords = route.getCoordinates();

            assertThat(coords).hasSize(4);
        }

        @Test
        @DisplayName("should extract coordinates from segment with multiple points")
        void shouldExtractCoordinatesFromSegmentWithMultiplePoints() {
            List<RoadSegment> segments = List.of(
                    new RoadSegment("seg1", List.of(
                            new Coordinate(52.0, 21.0),
                            new Coordinate(52.05, 21.05),
                            new Coordinate(52.08, 21.08),
                            new Coordinate(52.1, 21.1)
                    ), false)
            );
            RouteMetadata metadata = new RouteMetadata(100.0, 50L, 0, 1.0, Instant.now(), false);
            Route route = new Route(segments, metadata);

            List<Coordinate> coords = route.getCoordinates();

            assertThat(coords).hasSize(4);
        }
    }

    @Nested
    @DisplayName("Immutability tests")
    class ImmutabilityTests {

        @Test
        @DisplayName("should return defensive copy of segments")
        void shouldReturnDefensiveCopyOfSegments() {
            java.util.ArrayList<RoadSegment> segments = new java.util.ArrayList<>();
            segments.add(new RoadSegment("seg1", List.of(
                    new Coordinate(52.0, 21.0),
                    new Coordinate(52.1, 21.1)
            ), false));
            RouteMetadata metadata = new RouteMetadata(100.0, 50L, 0, 1.0, Instant.now(), false);
            Route route = new Route(segments, metadata);

            List<RoadSegment> retrievedSegments = route.getSegments();

            assertThat(retrievedSegments).isNotSameAs(segments);
            assertThatThrownBy(() -> retrievedSegments.add(
                    new RoadSegment("seg2", List.of(
                            new Coordinate(52.2, 21.2),
                            new Coordinate(52.3, 21.3)
                    ), false)
            )).isInstanceOf(UnsupportedOperationException.class);
        }
    }
}
