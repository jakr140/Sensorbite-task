package com.sensorbite.evacroute.domain.service;

import com.sensorbite.evacroute.domain.model.Coordinate;
import com.sensorbite.evacroute.domain.model.Graph;
import com.sensorbite.evacroute.domain.model.RoadSegment;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GraphBuilderTest {

    @Test
    void shouldBuildGraphFromSegments() {
        GraphBuilder builder = new GraphBuilder();

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
    void shouldCreateOnlyForwardEdgeForOnewaySegment() {
        GraphBuilder builder = new GraphBuilder();

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

    @Test
    void shouldReuseNodesForConnectedSegments() {
        GraphBuilder builder = new GraphBuilder();

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
        assertThat(graph.getEdgeCount()).isEqualTo(4);
    }
}
