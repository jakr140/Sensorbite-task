package com.sensorbite.evacroute.infrastructure.adapter.out.file;

import com.sensorbite.evacroute.domain.model.Coordinate;
import com.sensorbite.evacroute.domain.model.FloodZone;
import com.sensorbite.evacroute.domain.model.RoadSegment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JtsHazardDetectionAdapter")
class JtsHazardDetectionAdapterTest {

    private JtsHazardDetectionAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new JtsHazardDetectionAdapter();
    }

    @Nested
    @DisplayName("Spatial intersection detection")
    class SpatialIntersectionTests {

        @Test
        @DisplayName("should detect segment intersecting flood zone")
        void shouldDetectSegmentIntersectingFloodZone() {
            RoadSegment segment = new RoadSegment("seg1", List.of(
                    new Coordinate(52.0, 21.0),
                    new Coordinate(52.1, 21.1)
            ), false);

            FloodZone zone = new FloodZone("zone1", List.of(
                    List.of(
                            new Coordinate(52.05, 21.05),
                            new Coordinate(52.15, 21.05),
                            new Coordinate(52.15, 21.15),
                            new Coordinate(52.05, 21.15),
                            new Coordinate(52.05, 21.05)
                    )
            ), null, null);

            Set<String> hazardous = adapter.detectHazardousSegments(List.of(segment), List.of(zone));

            assertThat(hazardous).contains("seg1");
        }

        @Test
        @DisplayName("should not detect segment outside flood zone")
        void shouldNotDetectSegmentOutsideFloodZone() {
            RoadSegment segment = new RoadSegment("seg1", List.of(
                    new Coordinate(52.0, 21.0),
                    new Coordinate(52.01, 21.01)
            ), false);

            FloodZone zone = new FloodZone("zone1", List.of(
                    List.of(
                            new Coordinate(52.5, 21.5),
                            new Coordinate(52.6, 21.5),
                            new Coordinate(52.6, 21.6),
                            new Coordinate(52.5, 21.6),
                            new Coordinate(52.5, 21.5)
                    )
            ), null, null);

            Set<String> hazardous = adapter.detectHazardousSegments(List.of(segment), List.of(zone));

            assertThat(hazardous).isEmpty();
        }

        @Test
        @DisplayName("should detect segment completely inside flood zone")
        void shouldDetectSegmentCompletelyInsideFloodZone() {
            RoadSegment segment = new RoadSegment("seg1", List.of(
                    new Coordinate(52.06, 21.06),
                    new Coordinate(52.07, 21.07)
            ), false);

            FloodZone zone = new FloodZone("zone1", List.of(
                    List.of(
                            new Coordinate(52.05, 21.05),
                            new Coordinate(52.1, 21.05),
                            new Coordinate(52.1, 21.1),
                            new Coordinate(52.05, 21.1),
                            new Coordinate(52.05, 21.05)
                    )
            ), null, null);

            Set<String> hazardous = adapter.detectHazardousSegments(List.of(segment), List.of(zone));

            assertThat(hazardous).contains("seg1");
        }

        @Test
        @DisplayName("should detect segment partially intersecting flood zone")
        void shouldDetectSegmentPartiallyIntersectingFloodZone() {
            RoadSegment segment = new RoadSegment("seg1", List.of(
                    new Coordinate(52.0, 21.0),
                    new Coordinate(52.2, 21.2)
            ), false);

            FloodZone zone = new FloodZone("zone1", List.of(
                    List.of(
                            new Coordinate(52.08, 21.08),
                            new Coordinate(52.12, 21.08),
                            new Coordinate(52.12, 21.12),
                            new Coordinate(52.08, 21.12),
                            new Coordinate(52.08, 21.08)
                    )
            ), null, null);

            Set<String> hazardous = adapter.detectHazardousSegments(List.of(segment), List.of(zone));

            assertThat(hazardous).contains("seg1");
        }
    }

    @Nested
    @DisplayName("Multiple segments and zones")
    class MultipleSegmentsAndZonesTests {

        @Test
        @DisplayName("should detect multiple hazardous segments")
        void shouldDetectMultipleHazardousSegments() {
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

            FloodZone zone = new FloodZone("zone1", List.of(
                    List.of(
                            new Coordinate(51.95, 20.95),
                            new Coordinate(52.25, 20.95),
                            new Coordinate(52.25, 21.25),
                            new Coordinate(51.95, 21.25),
                            new Coordinate(51.95, 20.95)
                    )
            ), null, null);

            Set<String> hazardous = adapter.detectHazardousSegments(segments, List.of(zone));

            assertThat(hazardous).containsExactlyInAnyOrder("seg1", "seg2");
        }

        @Test
        @DisplayName("should handle multiple flood zones")
        void shouldHandleMultipleFloodZones() {
            RoadSegment segment = new RoadSegment("seg1", List.of(
                    new Coordinate(52.0, 21.0),
                    new Coordinate(52.2, 21.2)
            ), false);

            List<FloodZone> zones = List.of(
                    new FloodZone("zone1", List.of(
                            List.of(
                                    new Coordinate(52.05, 21.05),
                                    new Coordinate(52.08, 21.05),
                                    new Coordinate(52.08, 21.08),
                                    new Coordinate(52.05, 21.08),
                                    new Coordinate(52.05, 21.05)
                            )
                    ), null, null),
                    new FloodZone("zone2", List.of(
                            List.of(
                                    new Coordinate(52.12, 21.12),
                                    new Coordinate(52.15, 21.12),
                                    new Coordinate(52.15, 21.15),
                                    new Coordinate(52.12, 21.15),
                                    new Coordinate(52.12, 21.12)
                            )
                    ), null, null)
            );

            Set<String> hazardous = adapter.detectHazardousSegments(List.of(segment), zones);

            assertThat(hazardous).contains("seg1");
        }

        @Test
        @DisplayName("should filter out non-intersecting segments")
        void shouldFilterOutNonIntersectingSegments() {
            List<RoadSegment> segments = List.of(
                    new RoadSegment("seg1", List.of(
                            new Coordinate(52.0, 21.0),
                            new Coordinate(52.1, 21.1)
                    ), false),
                    new RoadSegment("seg2", List.of(
                            new Coordinate(52.5, 21.5),
                            new Coordinate(52.6, 21.6)
                    ), false)
            );

            FloodZone zone = new FloodZone("zone1", List.of(
                    List.of(
                            new Coordinate(52.05, 21.05),
                            new Coordinate(52.15, 21.05),
                            new Coordinate(52.15, 21.15),
                            new Coordinate(52.05, 21.15),
                            new Coordinate(52.05, 21.05)
                    )
            ), null, null);

            Set<String> hazardous = adapter.detectHazardousSegments(segments, List.of(zone));

            assertThat(hazardous).containsExactly("seg1");
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("should return empty set when no flood zones")
        void shouldReturnEmptySetWhenNoFloodZones() {
            RoadSegment segment = new RoadSegment("seg1", List.of(
                    new Coordinate(52.0, 21.0),
                    new Coordinate(52.1, 21.1)
            ), false);

            Set<String> hazardous = adapter.detectHazardousSegments(List.of(segment), List.of());

            assertThat(hazardous).isEmpty();
        }

        @Test
        @DisplayName("should return empty set when no segments")
        void shouldReturnEmptySetWhenNoSegments() {
            FloodZone zone = new FloodZone("zone1", List.of(
                    List.of(
                            new Coordinate(52.0, 21.0),
                            new Coordinate(52.1, 21.0),
                            new Coordinate(52.1, 21.1),
                            new Coordinate(52.0, 21.0)
                    )
            ), null, null);

            Set<String> hazardous = adapter.detectHazardousSegments(List.of(), List.of(zone));

            assertThat(hazardous).isEmpty();
        }

        @Test
        @DisplayName("should handle flood zone with holes")
        void shouldHandleFloodZoneWithHoles() {
            RoadSegment segment = new RoadSegment("seg1", List.of(
                    new Coordinate(52.1, 21.1),
                    new Coordinate(52.12, 21.12)
            ), false);

            FloodZone zone = new FloodZone("zone1", List.of(
                    List.of(
                            new Coordinate(52.0, 21.0),
                            new Coordinate(52.2, 21.0),
                            new Coordinate(52.2, 21.2),
                            new Coordinate(52.0, 21.2),
                            new Coordinate(52.0, 21.0)
                    ),
                    List.of(
                            new Coordinate(52.09, 21.09),
                            new Coordinate(52.11, 21.09),
                            new Coordinate(52.11, 21.11),
                            new Coordinate(52.09, 21.11),
                            new Coordinate(52.09, 21.09)
                    )
            ), null, null);

            Set<String> hazardous = adapter.detectHazardousSegments(List.of(segment), List.of(zone));

            assertThat(hazardous).contains("seg1");
        }
    }

    @Nested
    @DisplayName("Spatial indexing")
    class SpatialIndexingTests {

        @Test
        @DisplayName("should efficiently handle many segments and zones")
        void shouldEfficientlyHandleManySegmentsAndZones() {
            List<RoadSegment> segments = List.of(
                    new RoadSegment("seg1", List.of(
                            new Coordinate(52.0, 21.0),
                            new Coordinate(52.1, 21.1)
                    ), false),
                    new RoadSegment("seg2", List.of(
                            new Coordinate(52.2, 21.2),
                            new Coordinate(52.3, 21.3)
                    ), false),
                    new RoadSegment("seg3", List.of(
                            new Coordinate(52.4, 21.4),
                            new Coordinate(52.5, 21.5)
                    ), false)
            );

            List<FloodZone> zones = List.of(
                    new FloodZone("zone1", List.of(
                            List.of(
                                    new Coordinate(52.05, 21.05),
                                    new Coordinate(52.15, 21.05),
                                    new Coordinate(52.15, 21.15),
                                    new Coordinate(52.05, 21.15),
                                    new Coordinate(52.05, 21.05)
                            )
                    ), null, null),
                    new FloodZone("zone2", List.of(
                            List.of(
                                    new Coordinate(52.25, 21.25),
                                    new Coordinate(52.35, 21.25),
                                    new Coordinate(52.35, 21.35),
                                    new Coordinate(52.25, 21.35),
                                    new Coordinate(52.25, 21.25)
                            )
                    ), null, null)
            );

            Set<String> hazardous = adapter.detectHazardousSegments(segments, zones);

            assertThat(hazardous).containsExactlyInAnyOrder("seg1", "seg2");
        }
    }
}
