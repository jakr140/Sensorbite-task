package com.sensorbite.evacroute.application.service;

import com.sensorbite.evacroute.application.dto.RouteRequest;
import com.sensorbite.evacroute.application.dto.RouteResponse;
import com.sensorbite.evacroute.application.mapper.RouteMapper;
import com.sensorbite.evacroute.domain.model.Coordinate;
import com.sensorbite.evacroute.domain.model.FloodZone;
import com.sensorbite.evacroute.domain.model.RoadNetwork;
import com.sensorbite.evacroute.domain.model.Route;
import com.sensorbite.evacroute.domain.port.in.CalculateRouteUseCase;
import com.sensorbite.evacroute.domain.port.out.FloodZoneRepository;
import com.sensorbite.evacroute.domain.port.out.HazardDetectionPort;
import com.sensorbite.evacroute.domain.port.out.RoadNetworkRepository;
import com.sensorbite.evacroute.domain.service.RouteCalculationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class RouteApplicationService implements CalculateRouteUseCase {

    /**
     * Maximum allowed straight-line distance between start and end coordinates.
     *
     * <p>Value: 200,000 meters (200 km)</p>
     *
     * <p>Rationale: Limits computation time for large-scale routing. Evacuation
     * routes typically cover local/regional distances. Requests exceeding 200km
     * likely indicate coordinate errors or should be split into multiple segments.</p>
     */
    private static final double MAX_ROUTE_DISTANCE_METERS = 200_000.0;

    /**
     * Maximum route distance in kilometers (for user-facing messages).
     */
    private static final int MAX_ROUTE_DISTANCE_KM = 200;

    private final RoadNetworkRepository roadNetworkRepository;
    private final FloodZoneRepository floodZoneRepository;
    private final HazardDetectionPort hazardDetectionPort;
    private final RouteCalculationService routeCalculationService;
    private final RouteMapper routeMapper;

    public RouteResponse calculateRoute(RouteRequest request) {
        log.info("Calculating route from {} to {}", request.start(), request.end());

        Coordinate start = routeMapper.parseCoordinate(request.start());
        Coordinate end = routeMapper.parseCoordinate(request.end());

        validateDistance(start, end);

        RoadNetwork network = roadNetworkRepository.load();
        List<FloodZone> floodZones = floodZoneRepository.loadActiveAt(Instant.now());

        network.applyFloodZones(floodZones, hazardDetectionPort);

        Route route = routeCalculationService.calculateRoute(network, start, end);

        log.info("Route calculated: {} meters, {} segments, safety score: {}",
                route.getMetadata().distanceMeters(),
                route.getSegments().size(),
                route.getMetadata().safetyScore());

        return routeMapper.toResponse(route);
    }

    @Override
    public Route calculateRoute(Coordinate start, Coordinate end) {
        RoadNetwork network = roadNetworkRepository.load();
        List<FloodZone> floodZones = floodZoneRepository.loadActiveAt(Instant.now());
        network.applyFloodZones(floodZones, hazardDetectionPort);
        return routeCalculationService.calculateRoute(network, start, end);
    }

    private void validateDistance(Coordinate start, Coordinate end) {
        double straightLineDistance = start.distanceTo(end);

        if (straightLineDistance > MAX_ROUTE_DISTANCE_METERS) {
            throw new IllegalArgumentException(
                    String.format("Distance between start and end exceeds maximum: %d km (%.1f km requested)",
                        MAX_ROUTE_DISTANCE_KM, straightLineDistance / 1000.0)
            );
        }
    }
}
