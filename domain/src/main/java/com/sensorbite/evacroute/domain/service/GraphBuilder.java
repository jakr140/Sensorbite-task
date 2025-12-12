package com.sensorbite.evacroute.domain.service;

import com.sensorbite.evacroute.domain.model.Coordinate;
import com.sensorbite.evacroute.domain.model.Graph;
import com.sensorbite.evacroute.domain.model.RoadSegment;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GraphBuilder {

    /**
     * Distance threshold for merging nearby coordinates into a single graph node.
     *
     * <p>Value: 1.0 meters</p>
     *
     * <p>Coordinates within 1 meter are considered the same intersection point.
     * This tolerates minor GPS inaccuracies and coordinate rounding in source data
     * while avoiding false node merges. Increasing this value reduces node count
     * but may incorrectly merge distinct road endpoints.</p>
     */
    private static final double COORDINATE_TOLERANCE_METERS = 1.0;

    public Graph buildGraph(List<RoadSegment> segments) {
        Graph graph = new Graph();
        Map<String, Graph.Node> nodeCache = new HashMap<>();

        segments.forEach(segment -> {
            List<Coordinate> coords = segment.getCoordinates();
            Coordinate start = coords.getFirst();
            Coordinate end = coords.getLast();

            Graph.Node startNode = getOrCreateNode(start, nodeCache, graph);
            Graph.Node endNode = getOrCreateNode(end, nodeCache, graph);

            Graph.Edge forwardEdge = new Graph.Edge(
                startNode.id(),
                endNode.id(),
                segment.getLengthMeters(),
                segment.isHazardous(),
                segment.getId()
            );
            graph.addEdge(forwardEdge);

            if (!segment.isOneway()) {
                Graph.Edge backwardEdge = new Graph.Edge(
                    endNode.id(),
                    startNode.id(),
                    segment.getLengthMeters(),
                    segment.isHazardous(),
                    segment.getId()
                );
                graph.addEdge(backwardEdge);
            }
        });

        return graph;
    }

    private Graph.Node getOrCreateNode(Coordinate coord, Map<String, Graph.Node> nodeCache, Graph graph) {
        String key = findNearbyNodeKey(coord, nodeCache);
        if (key != null) {
            return nodeCache.get(key);
        }

        String nodeId = generateNodeId(coord);
        Graph.Node node = new Graph.Node(nodeId, coord);
        graph.addNode(node);
        nodeCache.put(nodeId, node);
        return node;
    }

    private String findNearbyNodeKey(Coordinate coord, Map<String, Graph.Node> nodeCache) {
        return nodeCache.entrySet().stream()
                .filter(entry -> entry.getValue().coordinate().distanceTo(coord) < COORDINATE_TOLERANCE_METERS)
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }

    private String generateNodeId(Coordinate coord) {
        return String.format("node_%.6f_%.6f", coord.latitude(), coord.longitude());
    }
}
