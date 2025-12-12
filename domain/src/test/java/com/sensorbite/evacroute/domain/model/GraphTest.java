package com.sensorbite.evacroute.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Graph")
class GraphTest {

    @Nested
    @DisplayName("Node operations")
    class NodeOperationsTests {

        @Test
        @DisplayName("should add node to graph")
        void shouldAddNodeToGraph() {
            Graph graph = new Graph();
            Graph.Node node = new Graph.Node("node1", new Coordinate(52.0, 21.0));

            graph.addNode(node);

            assertThat(graph.getNode("node1")).isPresent();
            assertThat(graph.getNodeCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("should retrieve node by id")
        void shouldRetrieveNodeById() {
            Graph graph = new Graph();
            Graph.Node node = new Graph.Node("node1", new Coordinate(52.0, 21.0));
            graph.addNode(node);

            Optional<Graph.Node> retrieved = graph.getNode("node1");

            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().id()).isEqualTo("node1");
            assertThat(retrieved.get().coordinate()).isEqualTo(new Coordinate(52.0, 21.0));
        }

        @Test
        @DisplayName("should return empty optional for non-existent node")
        void shouldReturnEmptyOptionalForNonExistentNode() {
            Graph graph = new Graph();

            Optional<Graph.Node> retrieved = graph.getNode("nonexistent");

            assertThat(retrieved).isEmpty();
        }

        @Test
        @DisplayName("should get all nodes")
        void shouldGetAllNodes() {
            Graph graph = new Graph();
            graph.addNode(new Graph.Node("node1", new Coordinate(52.0, 21.0)));
            graph.addNode(new Graph.Node("node2", new Coordinate(52.1, 21.1)));

            Map<String, Graph.Node> nodes = graph.getAllNodes();

            assertThat(nodes).hasSize(2);
            assertThat(nodes).containsKey("node1");
            assertThat(nodes).containsKey("node2");
        }

        @Test
        @DisplayName("should reject node with null id")
        void shouldRejectNodeWithNullId() {
            assertThatThrownBy(() -> new Graph.Node(null, new Coordinate(52.0, 21.0)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Node ID cannot be null or blank");
        }

        @Test
        @DisplayName("should reject node with blank id")
        void shouldRejectNodeWithBlankId() {
            assertThatThrownBy(() -> new Graph.Node("  ", new Coordinate(52.0, 21.0)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Node ID cannot be null or blank");
        }

        @Test
        @DisplayName("should reject node with null coordinate")
        void shouldRejectNodeWithNullCoordinate() {
            assertThatThrownBy(() -> new Graph.Node("node1", null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Coordinate cannot be null");
        }
    }

    @Nested
    @DisplayName("Edge operations")
    class EdgeOperationsTests {

        @Test
        @DisplayName("should add edge to graph")
        void shouldAddEdgeToGraph() {
            Graph graph = new Graph();
            Graph.Edge edge = new Graph.Edge("node1", "node2", 100.0, false, "seg1");

            graph.addEdge(edge);

            assertThat(graph.getEdges("node1")).hasSize(1);
            assertThat(graph.getEdgeCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("should retrieve edges for node")
        void shouldRetrieveEdgesForNode() {
            Graph graph = new Graph();
            Graph.Edge edge1 = new Graph.Edge("node1", "node2", 100.0, false, "seg1");
            Graph.Edge edge2 = new Graph.Edge("node1", "node3", 150.0, false, "seg2");
            graph.addEdge(edge1);
            graph.addEdge(edge2);

            List<Graph.Edge> edges = graph.getEdges("node1");

            assertThat(edges).hasSize(2);
        }

        @Test
        @DisplayName("should return empty list for node with no edges")
        void shouldReturnEmptyListForNodeWithNoEdges() {
            Graph graph = new Graph();

            List<Graph.Edge> edges = graph.getEdges("node1");

            assertThat(edges).isEmpty();
        }

        @Test
        @DisplayName("should reject edge with null from node id")
        void shouldRejectEdgeWithNullFromNodeId() {
            assertThatThrownBy(() -> new Graph.Edge(null, "node2", 100.0, false, "seg1"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("From node ID cannot be null or blank");
        }

        @Test
        @DisplayName("should reject edge with blank from node id")
        void shouldRejectEdgeWithBlankFromNodeId() {
            assertThatThrownBy(() -> new Graph.Edge("  ", "node2", 100.0, false, "seg1"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("From node ID cannot be null or blank");
        }

        @Test
        @DisplayName("should reject edge with null to node id")
        void shouldRejectEdgeWithNullToNodeId() {
            assertThatThrownBy(() -> new Graph.Edge("node1", null, 100.0, false, "seg1"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("To node ID cannot be null or blank");
        }

        @Test
        @DisplayName("should reject edge with negative weight")
        void shouldRejectEdgeWithNegativeWeight() {
            assertThatThrownBy(() -> new Graph.Edge("node1", "node2", -100.0, false, "seg1"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Edge weight cannot be negative");
        }

        @Test
        @DisplayName("should accept edge with zero weight")
        void shouldAcceptEdgeWithZeroWeight() {
            Graph.Edge edge = new Graph.Edge("node1", "node2", 0.0, false, "seg1");

            assertThat(edge.weight()).isZero();
        }

        @Test
        @DisplayName("should preserve hazardous flag")
        void shouldPreserveHazardousFlag() {
            Graph.Edge edge = new Graph.Edge("node1", "node2", 100.0, true, "seg1");

            assertThat(edge.hazardous()).isTrue();
        }

        @Test
        @DisplayName("should preserve segment id")
        void shouldPreserveSegmentId() {
            Graph.Edge edge = new Graph.Edge("node1", "node2", 100.0, false, "custom-segment-123");

            assertThat(edge.segmentId()).isEqualTo("custom-segment-123");
        }
    }

    @Nested
    @DisplayName("Graph metrics")
    class GraphMetricsTests {

        @Test
        @DisplayName("should count nodes correctly")
        void shouldCountNodesCorrectly() {
            Graph graph = new Graph();
            graph.addNode(new Graph.Node("node1", new Coordinate(52.0, 21.0)));
            graph.addNode(new Graph.Node("node2", new Coordinate(52.1, 21.1)));
            graph.addNode(new Graph.Node("node3", new Coordinate(52.2, 21.2)));

            assertThat(graph.getNodeCount()).isEqualTo(3);
        }

        @Test
        @DisplayName("should count edges correctly")
        void shouldCountEdgesCorrectly() {
            Graph graph = new Graph();
            graph.addEdge(new Graph.Edge("node1", "node2", 100.0, false, "seg1"));
            graph.addEdge(new Graph.Edge("node2", "node1", 100.0, false, "seg1"));
            graph.addEdge(new Graph.Edge("node2", "node3", 150.0, false, "seg2"));

            assertThat(graph.getEdgeCount()).isEqualTo(3);
        }

        @Test
        @DisplayName("should return zero for empty graph")
        void shouldReturnZeroForEmptyGraph() {
            Graph graph = new Graph();

            assertThat(graph.getNodeCount()).isZero();
            assertThat(graph.getEdgeCount()).isZero();
        }
    }

    @Nested
    @DisplayName("Multiple edges")
    class MultipleEdgesTests {

        @Test
        @DisplayName("should allow multiple edges from same node")
        void shouldAllowMultipleEdgesFromSameNode() {
            Graph graph = new Graph();
            graph.addEdge(new Graph.Edge("node1", "node2", 100.0, false, "seg1"));
            graph.addEdge(new Graph.Edge("node1", "node3", 150.0, false, "seg2"));
            graph.addEdge(new Graph.Edge("node1", "node4", 200.0, false, "seg3"));

            assertThat(graph.getEdges("node1")).hasSize(3);
        }

        @Test
        @DisplayName("should allow duplicate edges")
        void shouldAllowDuplicateEdges() {
            Graph graph = new Graph();
            graph.addEdge(new Graph.Edge("node1", "node2", 100.0, false, "seg1"));
            graph.addEdge(new Graph.Edge("node1", "node2", 100.0, false, "seg1"));

            assertThat(graph.getEdges("node1")).hasSize(2);
        }
    }
}
