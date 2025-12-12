package com.sensorbite.evacroute.domain.service;

import com.sensorbite.evacroute.domain.model.Coordinate;
import com.sensorbite.evacroute.domain.model.Graph;
import com.sensorbite.evacroute.domain.model.RoadSegment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GraphBuilder Edge Cases")
class GraphBuilderEdgeCasesTest {

    private GraphBuilder builder;

    @BeforeEach
    void setUp() {
        builder = new GraphBuilder();
    }

    @Nested
    @DisplayName("Empty and single segment tests")
    class EmptyAndSingleTests {

        @Test
        @DisplayName("should build empty graph from empty segment list")
        void shouldBuildEmptyGraphFromEmptyList() {
            Graph graph = builder.buildGraph(List.of());

            assertThat(graph.getNodeCount()).isZero();
            assertThat(graph.getEdgeCount()).isZero();
        }

        @Test
        @DisplayName("should build graph from single segment")
        void shouldBuildGraphFromSingleSegment() {
            List<RoadSegment> segments = List.of(
                    new RoadSegment("seg1", List.of(
                            new Coordinate(52.0, 21.0),
                            new Coordinate(52.1, 21.1)
                    ), false)
            );

            Graph graph = builder.buildGraph(segments);

            assertThat(graph.getNodeCount()).isEqualTo(2);
            assertThat(graph.getEdgeCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("should build graph from single oneway segment")
        void shouldBuildGraphFromSingleOnewaySegment() {
            List<RoadSegment> segments = List.of(
                    new RoadSegment("seg1", List.of(
                            new Coordinate(52.0, 21.0),
                            new Coordinate(52.1, 21.1)
                    ), true)
            );

            Graph graph = builder.buildGraph(segments);

            assertThat(graph.getNodeCount()).isEqualTo(2);
            assertThat(graph.getEdgeCount()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Duplicate handling")
    class DuplicateTests {

        @Test
        @DisplayName("should merge duplicate segments")
        void shouldMergeDuplicateSegments() {
            List<RoadSegment> segments = List.of(
                    new RoadSegment("seg1", List.of(
                            new Coordinate(52.0, 21.0),
                            new Coordinate(52.1, 21.1)
                    ), false),
                    new RoadSegment("seg2", List.of(
                            new Coordinate(52.0, 21.0),
                            new Coordinate(52.1, 21.1)
                    ), false)
            );

            Graph graph = builder.buildGraph(segments);

            assertThat(graph.getNodeCount()).isEqualTo(2);
            assertThat(graph.getEdgeCount()).isEqualTo(4);
        }

        @Test
        @DisplayName("should merge nearby coordinates within tolerance")
        void shouldMergeNearbyCoordinatesWithinTolerance() {
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

            Graph graph = builder.buildGraph(segments);

            assertThat(graph.getNodeCount()).isEqualTo(3);
        }

        @Test
        @DisplayName("should not merge coordinates beyond tolerance")
        void shouldNotMergeCoordinatesBeyondTolerance() {
            List<RoadSegment> segments = List.of(
                    new RoadSegment("seg1", List.of(
                            new Coordinate(52.0, 21.0),
                            new Coordinate(52.1, 21.1)
                    ), false),
                    new RoadSegment("seg2", List.of(
                            new Coordinate(52.0001, 21.0001),
                            new Coordinate(52.1001, 21.1001)
                    ), false)
            );

            Graph graph = builder.buildGraph(segments);

            assertThat(graph.getNodeCount()).isEqualTo(4);
        }
    }

    @Nested
    @DisplayName("Multi-point segments")
    class MultiPointSegmentTests {

        @Test
        @DisplayName("should only use first and last coordinates of segment")
        void shouldOnlyUseFirstAndLastCoordinates() {
            List<RoadSegment> segments = List.of(
                    new RoadSegment("seg1", List.of(
                            new Coordinate(52.0, 21.0),
                            new Coordinate(52.05, 21.05),
                            new Coordinate(52.08, 21.08),
                            new Coordinate(52.1, 21.1)
                    ), false)
            );

            Graph graph = builder.buildGraph(segments);

            assertThat(graph.getNodeCount()).isEqualTo(2);
            assertThat(graph.getEdgeCount()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("Complex graph scenarios")
    class ComplexGraphTests {

        @Test
        @DisplayName("should build graph with multiple connected segments")
        void shouldBuildGraphWithMultipleConnectedSegments() {
            List<RoadSegment> segments = List.of(
                    new RoadSegment("seg1", List.of(
                            new Coordinate(52.0, 21.0),
                            new Coordinate(52.1, 21.1)
                    ), false),
                    new RoadSegment("seg2", List.of(
                            new Coordinate(52.1, 21.1),
                            new Coordinate(52.2, 21.2)
                    ), false),
                    new RoadSegment("seg3", List.of(
                            new Coordinate(52.2, 21.2),
                            new Coordinate(52.3, 21.3)
                    ), false)
            );

            Graph graph = builder.buildGraph(segments);

            assertThat(graph.getNodeCount()).isEqualTo(4);
            assertThat(graph.getEdgeCount()).isEqualTo(6);
        }

        @Test
        @DisplayName("should build graph with branching paths")
        void shouldBuildGraphWithBranchingPaths() {
            List<RoadSegment> segments = List.of(
                    new RoadSegment("seg1", List.of(
                            new Coordinate(52.0, 21.0),
                            new Coordinate(52.1, 21.1)
                    ), false),
                    new RoadSegment("seg2", List.of(
                            new Coordinate(52.1, 21.1),
                            new Coordinate(52.2, 21.2)
                    ), false),
                    new RoadSegment("seg3", List.of(
                            new Coordinate(52.1, 21.1),
                            new Coordinate(52.2, 21.0)
                    ), false)
            );

            Graph graph = builder.buildGraph(segments);

            assertThat(graph.getNodeCount()).isEqualTo(4);
            assertThat(graph.getEdgeCount()).isEqualTo(6);
            assertThat(graph.getEdges("node_52.100000_21.100000")).hasSizeGreaterThanOrEqualTo(2);
        }

        @Test
        @DisplayName("should preserve hazardous flag in edges")
        void shouldPreserveHazardousFlagInEdges() {
            List<RoadSegment> segments = List.of(
                    new RoadSegment("seg1", List.of(
                            new Coordinate(52.0, 21.0),
                            new Coordinate(52.1, 21.1)
                    ), false, true)
            );

            Graph graph = builder.buildGraph(segments);

            List<Graph.Edge> edges = graph.getEdges("node_52.000000_21.000000");
            assertThat(edges).hasSize(1);
            assertThat(edges.get(0).hazardous()).isTrue();
        }

        @Test
        @DisplayName("should set correct segment ID in edges")
        void shouldSetCorrectSegmentIdInEdges() {
            List<RoadSegment> segments = List.of(
                    new RoadSegment("custom-id-123", List.of(
                            new Coordinate(52.0, 21.0),
                            new Coordinate(52.1, 21.1)
                    ), false)
            );

            Graph graph = builder.buildGraph(segments);

            List<Graph.Edge> edges = graph.getEdges("node_52.000000_21.000000");
            assertThat(edges.get(0).segmentId()).isEqualTo("custom-id-123");
        }
    }
}
