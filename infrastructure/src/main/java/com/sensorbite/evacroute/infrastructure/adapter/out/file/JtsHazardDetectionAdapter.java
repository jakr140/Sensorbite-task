package com.sensorbite.evacroute.infrastructure.adapter.out.file;

import com.sensorbite.evacroute.domain.model.Coordinate;
import com.sensorbite.evacroute.domain.model.FloodZone;
import com.sensorbite.evacroute.domain.model.RoadSegment;
import com.sensorbite.evacroute.domain.port.out.HazardDetectionPort;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.index.strtree.STRtree;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
public class JtsHazardDetectionAdapter implements HazardDetectionPort {

    private final GeometryFactory geometryFactory = new GeometryFactory();

    @Override
    public Set<String> detectHazardousSegments(Collection<RoadSegment> segments, List<FloodZone> zones) {
        if (zones.isEmpty()) {
            log.debug("No flood zones to process");
            return Set.of();
        }

        log.debug("Detecting hazardous segments: {} segments, {} flood zones", segments.size(), zones.size());

        STRtree spatialIndex = buildSpatialIndex(zones);

        Set<String> hazardousIds = segments.stream()
                .filter(segment -> {
                    LineString lineString = toJtsLineString(segment.getCoordinates());

                    @SuppressWarnings("unchecked")
                    List<Integer> candidateIndices = spatialIndex.query(lineString.getEnvelopeInternal());

                    return candidateIndices.stream()
                            .anyMatch(zoneIndex -> {
                                FloodZone zone = zones.get(zoneIndex);
                                Polygon polygon = toJtsPolygon(zone.polygonRings());
                                return polygon.intersects(lineString);
                            });
                })
                .map(RoadSegment::getId)
                .collect(java.util.stream.Collectors.toSet());

        log.debug("Detected {} hazardous segments", hazardousIds.size());
        return hazardousIds;
    }

    private STRtree buildSpatialIndex(List<FloodZone> zones) {
        STRtree rtree = new STRtree();
        java.util.stream.IntStream.range(0, zones.size())
                .forEach(i -> {
                    Polygon polygon = toJtsPolygon(zones.get(i).polygonRings());
                    rtree.insert(polygon.getEnvelopeInternal(), i);
                });
        rtree.build();
        return rtree;
    }

    private LineString toJtsLineString(List<Coordinate> coordinates) {
        org.locationtech.jts.geom.Coordinate[] jtsCoords = coordinates.stream()
                .map(coord -> new org.locationtech.jts.geom.Coordinate(coord.longitude(), coord.latitude()))
                .toArray(org.locationtech.jts.geom.Coordinate[]::new);
        return geometryFactory.createLineString(jtsCoords);
    }

    private Polygon toJtsPolygon(List<List<Coordinate>> rings) {
        org.locationtech.jts.geom.LinearRing shell = toJtsLinearRing(rings.getFirst());

        org.locationtech.jts.geom.LinearRing[] holes = rings.stream()
                .skip(1)
                .map(this::toJtsLinearRing)
                .toArray(org.locationtech.jts.geom.LinearRing[]::new);

        return geometryFactory.createPolygon(shell, holes);
    }

    private org.locationtech.jts.geom.LinearRing toJtsLinearRing(List<Coordinate> coordinates) {
        org.locationtech.jts.geom.Coordinate[] jtsCoords = coordinates.stream()
                .map(coord -> new org.locationtech.jts.geom.Coordinate(coord.longitude(), coord.latitude()))
                .toArray(org.locationtech.jts.geom.Coordinate[]::new);
        return geometryFactory.createLinearRing(jtsCoords);
    }
}
