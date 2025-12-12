package com.sensorbite.evacroute.application.mapper;

import com.sensorbite.evacroute.application.dto.GeometryDto;
import com.sensorbite.evacroute.application.dto.PropertiesDto;
import com.sensorbite.evacroute.application.dto.RouteResponse;
import com.sensorbite.evacroute.domain.exception.InvalidCoordinateException;
import com.sensorbite.evacroute.domain.model.Coordinate;
import com.sensorbite.evacroute.domain.model.Route;
import org.mapstruct.Mapper;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Mapper(componentModel = "default")
public interface RouteMapper {

    String ROUTE_GEOMETRY_TYPE = "LineString";
    String GEOJSON_TYPE_FEATURE = "Feature";

    /**
     * Coordinate precision (decimal places) for input parsing.
     * 6 decimal places = ~0.11 meter precision at equator.
     */
    int COORDINATE_PRECISION = 6;

    default RouteResponse toResponse(Route route) {
        GeometryDto geometry = new GeometryDto(
                ROUTE_GEOMETRY_TYPE,
                route.getCoordinates().stream()
                        .map(coord -> List.of(coord.longitude(), coord.latitude()))
                        .toList()
        );

        PropertiesDto properties = new PropertiesDto(
                route.getMetadata().distanceMeters(),
                route.getMetadata().computationTimeMs(),
                route.getMetadata().hazardousSegmentsAvoided(),
                route.getMetadata().safetyScore(),
                route.getMetadata().timestamp().toString(),
                route.getMetadata().allPathsHazardous()
        );

        return new RouteResponse(GEOJSON_TYPE_FEATURE, geometry, properties);
    }

    default Coordinate parseCoordinate(String coordString) {
        if (coordString == null || coordString.isBlank()) {
            throw new InvalidCoordinateException("Coordinate string cannot be null or blank");
        }

        List<String> parts = List.of(coordString.trim().split(","));
        if (parts.size() != 2) {
            throw new InvalidCoordinateException("Invalid coordinate format. Expected: 'lat,lon'");
        }

        try {
            double lat = Double.parseDouble(parts.get(0).trim());
            double lon = Double.parseDouble(parts.get(1).trim());
            return new Coordinate(roundToPrecision(lat, COORDINATE_PRECISION), roundToPrecision(lon, COORDINATE_PRECISION));
        } catch (NumberFormatException e) {
            throw new InvalidCoordinateException("Invalid coordinate numbers: " + coordString);
        }
    }

    private double roundToPrecision(double value, int decimalPlaces) {
        double scale = Math.pow(10, decimalPlaces);
        return Math.round(value * scale) / scale;
    }
}
