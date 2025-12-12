package com.sensorbite.evacroute.domain.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class Graph {
    private final Map<String, Node> nodes;
    private final Map<String, List<Edge>> adjacencyList;

    public Graph() {
        this.nodes = new HashMap<>();
        this.adjacencyList = new HashMap<>();
    }

    public void addNode(Node node) {
        nodes.put(node.id(), node);
        adjacencyList.putIfAbsent(node.id(), new ArrayList<>());
    }

    public void addEdge(Edge edge) {
        adjacencyList.computeIfAbsent(edge.fromNodeId(), k -> new ArrayList<>()).add(edge);
    }

    public Optional<Node> getNode(String nodeId) {
        return Optional.ofNullable(nodes.get(nodeId));
    }

    public List<Edge> getEdges(String nodeId) {
        return adjacencyList.getOrDefault(nodeId, List.of());
    }

    public Map<String, Node> getAllNodes() {
        return Map.copyOf(nodes);
    }

    public int getNodeCount() {
        return nodes.size();
    }

    public int getEdgeCount() {
        return adjacencyList.values().stream()
                .mapToInt(List::size)
                .sum();
    }

    public record Node(String id, Coordinate coordinate) {
        public Node {
            if (id == null || id.isBlank()) {
                throw new IllegalArgumentException("Node ID cannot be null or blank");
            }
            if (coordinate == null) {
                throw new IllegalArgumentException("Coordinate cannot be null");
            }
        }
    }

    public record Edge(
        String fromNodeId,
        String toNodeId,
        double weight,
        boolean hazardous,
        String segmentId
    ) {
        public Edge {
            if (fromNodeId == null || fromNodeId.isBlank()) {
                throw new IllegalArgumentException("From node ID cannot be null or blank");
            }
            if (toNodeId == null || toNodeId.isBlank()) {
                throw new IllegalArgumentException("To node ID cannot be null or blank");
            }
            if (weight < 0) {
                throw new IllegalArgumentException("Edge weight cannot be negative");
            }
        }
    }
}
