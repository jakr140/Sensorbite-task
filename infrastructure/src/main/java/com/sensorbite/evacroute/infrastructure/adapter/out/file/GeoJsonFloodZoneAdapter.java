package com.sensorbite.evacroute.infrastructure.adapter.out.file;

import com.sensorbite.evacroute.domain.model.Coordinate;
import com.sensorbite.evacroute.domain.model.FloodZone;
import com.sensorbite.evacroute.domain.port.out.FloodZoneRepository;
import lombok.extern.slf4j.Slf4j;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class GeoJsonFloodZoneAdapter implements FloodZoneRepository {

    @Value("${routing.data.flood-zones-path}")
    private String floodZonesPath;

    @Override
    public List<FloodZone> loadActiveAt(Instant timestamp) {
        log.info("[DATA_LOAD] Loading flood zones from: {}", floodZonesPath);
        long startTime = System.currentTimeMillis();

        try {
            File file = new File(floodZonesPath);
            if (!file.exists()) {
                log.warn("Flood zones file not found: {}, assuming no flood zones", floodZonesPath);
                return List.of();
            }

            FeatureJSON featureJSON = new FeatureJSON();
            FeatureCollection<?, ?> features = featureJSON.readFeatureCollection(file);

            List<FloodZone> zones = new ArrayList<>();
            try (FeatureIterator<?> iterator = features.features()) {
                while (iterator.hasNext()) {
                    SimpleFeature feature = (SimpleFeature) iterator.next();
                    parseFeature(feature).ifPresent(zones::add);
                }
            }

            List<FloodZone> activeZones = zones.stream()
                    .filter(zone -> zone.isValidAt(timestamp))
                    .toList();

            long duration = System.currentTimeMillis() - startTime;
            log.info("[DATA_LOAD] Loaded {} flood zones ({} active at {}) in {} ms",
                    zones.size(), activeZones.size(), timestamp, duration);

            return activeZones;
        } catch (IOException e) {
            log.error("Failed to load flood zones from: {}", floodZonesPath, e);
            return List.of();
        }
    }

    private Optional<FloodZone> parseFeature(SimpleFeature feature) {
        Geometry geom = (Geometry) feature.getDefaultGeometry();
        if (geom == null || !GeometryType.POLYGON.matches(geom.getGeometryType())) {
            return Optional.empty();
        }

        Polygon polygon = (Polygon) geom;
        String featureId = feature.getID();

        List<List<Coordinate>> rings = new ArrayList<>();
        rings.add(convertRing(polygon.getExteriorRing().getCoordinates()));

        List<List<Coordinate>> interiorRings = java.util.stream.IntStream.range(0, polygon.getNumInteriorRing())
                .mapToObj(i -> convertRing(polygon.getInteriorRingN(i).getCoordinates()))
                .toList();
        rings.addAll(interiorRings);

        Instant validFrom = parseInstant(feature, GeoJsonProperty.VALID_FROM);
        Instant validUntil = parseInstant(feature, GeoJsonProperty.VALID_UNTIL);

        return Optional.of(new FloodZone(featureId, rings, validFrom, validUntil));
    }

    private List<Coordinate> convertRing(org.locationtech.jts.geom.Coordinate[] jtsCoords) {
        return java.util.Arrays.stream(jtsCoords)
                .map(jtsCoord -> new Coordinate(jtsCoord.y, jtsCoord.x))
                .toList();
    }

    private Instant parseInstant(SimpleFeature feature, String attributeName) {
        return Optional.ofNullable(feature.getAttribute(attributeName))
                .map(Object::toString)
                .map(Instant::parse)
                .orElse(null);
    }
}
