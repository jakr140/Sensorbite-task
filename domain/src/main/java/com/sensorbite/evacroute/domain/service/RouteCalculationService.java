package com.sensorbite.evacroute.domain.service;

import com.sensorbite.evacroute.domain.exception.RouteNotFoundException;
import com.sensorbite.evacroute.domain.model.Coordinate;
import com.sensorbite.evacroute.domain.model.Graph;
import com.sensorbite.evacroute.domain.model.RoadNetwork;
import com.sensorbite.evacroute.domain.model.RoadSegment;
import com.sensorbite.evacroute.domain.model.Route;
import com.sensorbite.evacroute.domain.model.RouteMetadata;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

public class RouteCalculationService {

    /**
     * Weight multiplier applied to hazardous road segments.
     *
     * <p>A factor of 10,000 makes hazardous segments effectively "last resort"
     * routes. For a typical 100m segment (weight=100), the hazardous weight
     * becomes 1,000,000, forcing Dijkstra to prefer any non-hazardous alternative
     * unless no other path exists.</p>
     *
     * <p>Rationale: 10,000x penalty means a 10km detour is preferred over 1m
     * of hazardous road, which aligns with evacuation safety priorities.</p>
     */
    private static final double HAZARD_PENALTY_FACTOR = 10_000.0;

    /**
     * Default safety score for an empty route (start == end).
     * Value of 1.0 indicates maximum safety (no road segments traversed).
     */
    private static final double EMPTY_ROUTE_SAFETY_SCORE = 1.0;

    public Route calculateRoute(RoadNetwork network, Coordinate start, Coordinate end) {
        long startTime = System.currentTimeMillis();

        Graph.Node startNode = network.findNearestNode(start)
                .orElseThrow(() -> new RouteNotFoundException("No road network near start coordinate"));
        Graph.Node endNode = network.findNearestNode(end)
                .orElseThrow(() -> new RouteNotFoundException("No road network near end coordinate"));

        if (startNode.id().equals(endNode.id())) {
            return createEmptyRoute(startTime);
        }

        DijkstraResult result = runDijkstra(network.getGraph(), startNode.id(), endNode.id());
        List<String> path = result.path();

        if (path.isEmpty()) {
            throw new RouteNotFoundException("No route available between specified points");
        }

        List<RoadSegment> routeSegments = reconstructSegments(path, network.getGraph(), network);
        RouteMetadata metadata = createMetadata(
                routeSegments,
                startTime,
                result.hasHazardousEdges()
        );

        return new Route(routeSegments, metadata);
    }

    private DijkstraResult runDijkstra(Graph graph, String startNodeId, String endNodeId) {
        Map<String, Double> distances = new HashMap<>();
        Map<String, String> predecessors = new HashMap<>();
        Set<String> visited = new HashSet<>();
        PriorityQueue<NodeDistance> priorityQueue = new PriorityQueue<>(Comparator.comparingDouble(NodeDistance::distance));

        distances.put(startNodeId, 0.0);
        priorityQueue.offer(new NodeDistance(startNodeId, 0.0));

        boolean hasHazardousEdges = false;

        while (!priorityQueue.isEmpty()) {
            NodeDistance current = priorityQueue.poll();

            if (current.nodeId().equals(endNodeId)) {
                break;
            }

            if (visited.contains(current.nodeId())) {
                continue;
            }
            visited.add(current.nodeId());

            for (Graph.Edge edge : graph.getEdges(current.nodeId())) {
                double weight = edge.weight();
                if (edge.hazardous()) {
                    weight *= HAZARD_PENALTY_FACTOR;
                    hasHazardousEdges = true;
                }

                double newDistance = current.distance() + weight;
                double currentDistance = distances.getOrDefault(edge.toNodeId(), Double.MAX_VALUE);

                if (newDistance < currentDistance) {
                    distances.put(edge.toNodeId(), newDistance);
                    predecessors.put(edge.toNodeId(), current.nodeId());
                    priorityQueue.offer(new NodeDistance(edge.toNodeId(), newDistance));
                }
            }
        }

        List<String> path = reconstructPath(predecessors, startNodeId, endNodeId);
        return new DijkstraResult(path, hasHazardousEdges);
    }

    private List<String> reconstructPath(Map<String, String> predecessors, String startNodeId, String endNodeId) {
        if (!predecessors.containsKey(endNodeId)) {
            return List.of();
        }

        List<String> path = new ArrayList<>();
        String current = endNodeId;
        while (!current.equals(startNodeId)) {
            path.add(current);
            current = predecessors.get(current);
        }
        path.add(startNodeId);

        return path.reversed();
    }

    private List<RoadSegment> reconstructSegments(List<String> path, Graph graph, RoadNetwork network) {
        return java.util.stream.IntStream.range(0, path.size() - 1)
                .mapToObj(i -> {
                    String fromNodeId = path.get(i);
                    String toNodeId = path.get(i + 1);

                    Graph.Edge edge = graph.getEdges(fromNodeId).stream()
                            .filter(e -> e.toNodeId().equals(toNodeId))
                            .findFirst()
                            .orElseThrow(() -> new IllegalStateException("Edge not found in path"));

                    return network.findSegment(edge.segmentId());
                })
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::get)
                .toList();
    }

    private RouteMetadata createMetadata(List<RoadSegment> segments, long startTime, boolean hasHazardousEdges) {
        double totalDistance = segments.stream()
                .mapToDouble(RoadSegment::getLengthMeters)
                .sum();

        long hazardousCount = segments.stream()
                .filter(RoadSegment::isHazardous)
                .count();

        boolean allHazardous = !segments.isEmpty() && hazardousCount == segments.size();
        double safetyScore = segments.isEmpty()
            ? EMPTY_ROUTE_SAFETY_SCORE
            : 1.0 - ((double) hazardousCount / segments.size());

        return new RouteMetadata(
                totalDistance,
                System.currentTimeMillis() - startTime,
                (int) hazardousCount,
                safetyScore,
                Instant.now(),
                allHazardous
        );
    }

    private Route createEmptyRoute(long startTime) {
        RouteMetadata metadata = new RouteMetadata(
                0.0,
                System.currentTimeMillis() - startTime,
                0,
                EMPTY_ROUTE_SAFETY_SCORE,
                Instant.now(),
                false
        );
        return new Route(List.of(), metadata);
    }

    private record NodeDistance(String nodeId, double distance) {}
    private record DijkstraResult(List<String> path, boolean hasHazardousEdges) {}
}
