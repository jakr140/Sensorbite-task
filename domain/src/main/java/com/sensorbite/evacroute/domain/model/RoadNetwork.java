package com.sensorbite.evacroute.domain.model;

import com.sensorbite.evacroute.domain.port.out.HazardDetectionPort;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class RoadNetwork {
    private final Map<String, RoadSegment> segments;
    private final Graph graph;

    public RoadNetwork(List<RoadSegment> segments, Graph graph) {
        if (segments == null || segments.isEmpty()) {
            throw new IllegalArgumentException("Road network must have at least one segment");
        }
        this.segments = segments.stream()
                .collect(Collectors.toMap(RoadSegment::getId, Function.identity()));
        this.graph = graph;
    }

    public void applyFloodZones(List<FloodZone> zones, HazardDetectionPort hazardDetector) {
        Set<String> hazardousIds = hazardDetector.detectHazardousSegments(segments.values(), zones);
        hazardousIds.forEach(id -> {
            RoadSegment segment = segments.get(id);
            if (segment != null) {
                segments.put(id, segment.withHazardous(true));
            }
        });
    }

    public Graph getGraph() {
        return graph;
    }

    public Collection<RoadSegment> getSegments() {
        return segments.values();
    }

    public Optional<RoadSegment> findSegment(String id) {
        return Optional.ofNullable(segments.get(id));
    }

    public Optional<Graph.Node> findNearestNode(Coordinate coord) {
        return graph.getAllNodes().values().stream()
                .min(Comparator.comparingDouble(node ->
                    node.coordinate().distanceTo(coord)));
    }
}
