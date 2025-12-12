package com.sensorbite.evacroute.infrastructure.adapter.out.file;

import com.sensorbite.evacroute.domain.model.Coordinate;
import com.sensorbite.evacroute.domain.model.Graph;
import com.sensorbite.evacroute.domain.model.RoadNetwork;
import com.sensorbite.evacroute.domain.model.RoadSegment;
import com.sensorbite.evacroute.domain.port.out.RoadNetworkRepository;
import com.sensorbite.evacroute.domain.service.GraphBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class GeoJsonRoadNetworkAdapter implements RoadNetworkRepository {

    @Value("${routing.data.road-network-path}")
    private String roadNetworkPath;

    private final GraphBuilder graphBuilder;

    @Override
    public RoadNetwork load() {
        log.info("[DATA_LOAD] Loading road network from: {}", roadNetworkPath);
        long startTime = System.currentTimeMillis();

        try {
            File file = new File(roadNetworkPath);
            if (!file.exists()) {
                throw new IllegalStateException("Road network file not found: " + roadNetworkPath);
            }

            FeatureJSON featureJSON = new FeatureJSON();
            FeatureCollection<?, ?> features = featureJSON.readFeatureCollection(file);

            List<RoadSegment> segments = new ArrayList<>();
            try (FeatureIterator<?> iterator = features.features()) {
                while (iterator.hasNext()) {
                    SimpleFeature feature = (SimpleFeature) iterator.next();
                    segments.addAll(parseFeature(feature));
                }
            }

            if (segments.isEmpty()) {
                throw new IllegalStateException("No valid road segments found in: " + roadNetworkPath);
            }

            Graph graph = graphBuilder.buildGraph(segments);
            RoadNetwork network = new RoadNetwork(segments, graph);

            long duration = System.currentTimeMillis() - startTime;
            log.info("[DATA_LOAD] Loaded {} segments, {} nodes, {} edges in {} ms",
                    segments.size(), graph.getNodeCount(), graph.getEdgeCount(), duration);

            return network;
        } catch (IOException e) {
            throw new RuntimeException("Failed to load road network from: " + roadNetworkPath, e);
        }
    }

    private List<RoadSegment> parseFeature(SimpleFeature feature) {
        Geometry geom = (Geometry) feature.getDefaultGeometry();
        if (geom == null) {
            return List.of();
        }

        String featureId = feature.getID();
        boolean oneway = parseOneway(feature);

        return switch (GeometryType.fromGeoJsonName(geom.getGeometryType())) {
            case LINE_STRING -> List.of(parseLineString((LineString) geom, featureId, oneway));
            case MULTI_LINE_STRING -> parseMultiLineString((MultiLineString) geom, featureId, oneway);
            case null, default -> {
                log.warn("Unsupported geometry type: {}", geom.getGeometryType());
                yield List.of();
            }
        };
    }

    private RoadSegment parseLineString(LineString lineString, String id, boolean oneway) {
        List<Coordinate> coordinates = java.util.Arrays.stream(lineString.getCoordinates())
                .map(jtsCoord -> new Coordinate(jtsCoord.y, jtsCoord.x))
                .toList();
        return new RoadSegment(id, coordinates, oneway);
    }

    private List<RoadSegment> parseMultiLineString(MultiLineString multiLineString, String baseId, boolean oneway) {
        return java.util.stream.IntStream.range(0, multiLineString.getNumGeometries())
                .mapToObj(i -> {
                    LineString lineString = (LineString) multiLineString.getGeometryN(i);
                    String segmentId = baseId + "_" + i;
                    return parseLineString(lineString, segmentId, oneway);
                })
                .toList();
    }

    private boolean parseOneway(SimpleFeature feature) {
        return Optional.ofNullable(feature.getAttribute(GeoJsonProperty.ONEWAY))
                .map(Object::toString)
                .map(GeoJsonProperty::parseBoolean)
                .orElse(false);
    }
}
