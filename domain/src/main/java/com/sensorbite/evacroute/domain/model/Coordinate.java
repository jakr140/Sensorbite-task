package com.sensorbite.evacroute.domain.model;

import com.sensorbite.evacroute.domain.exception.InvalidCoordinateException;

public record Coordinate(double latitude, double longitude) {

    /** Minimum valid latitude in decimal degrees (WGS84). */
    public static final double MIN_LATITUDE = -90.0;

    /** Maximum valid latitude in decimal degrees (WGS84). */
    public static final double MAX_LATITUDE = 90.0;

    /** Minimum valid longitude in decimal degrees (WGS84). */
    public static final double MIN_LONGITUDE = -180.0;

    /** Maximum valid longitude in decimal degrees (WGS84). */
    public static final double MAX_LONGITUDE = 180.0;

    /**
     * Earth's mean radius in meters.
     *
     * <p>Value: 6,371,000 meters (6,371 km)</p>
     *
     * <p>This is the WGS84 mean radius. The Haversine formula using this constant
     * provides distance accuracy of ±0.5% for most practical purposes. For
     * evacuation routing (where errors of 10-50m are acceptable), this precision
     * is sufficient.</p>
     *
     * @see <a href="https://en.wikipedia.org/wiki/Earth_radius">Earth radius on Wikipedia</a>
     */
    private static final double EARTH_RADIUS_METERS = 6_371_000.0;

    public Coordinate {
        if (latitude < MIN_LATITUDE || latitude > MAX_LATITUDE) {
            throw new InvalidCoordinateException(
                String.format("Latitude must be [%.1f, %.1f], got: %.6f",
                    MIN_LATITUDE, MAX_LATITUDE, latitude)
            );
        }
        if (longitude < MIN_LONGITUDE || longitude > MAX_LONGITUDE) {
            throw new InvalidCoordinateException(
                String.format("Longitude must be [%.1f, %.1f], got: %.6f",
                    MIN_LONGITUDE, MAX_LONGITUDE, longitude)
            );
        }
    }

    /**
     * Calculate distance to another coordinate using the Haversine formula.
     *
     * <p>The Haversine formula determines the great-circle distance between two
     * points on a sphere given their longitudes and latitudes.</p>
     *
     * @param other the target coordinate
     * @return distance in meters
     * @see <a href="https://en.wikipedia.org/wiki/Haversine_formula">Haversine formula</a>
     */
    public double distanceTo(Coordinate other) {
        double lat1Rad = Math.toRadians(this.latitude);
        double lat2Rad = Math.toRadians(other.latitude);
        double dLat = Math.toRadians(other.latitude - this.latitude);
        double dLon = Math.toRadians(other.longitude - this.longitude);

        if (Math.abs(dLon) > Math.PI) {
            dLon = dLon > 0 ? dLon - 2 * Math.PI : dLon + 2 * Math.PI;
        }

        double haversineA = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                            Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                            Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double centralAngle = 2 * Math.atan2(Math.sqrt(haversineA), Math.sqrt(1 - haversineA));

        return EARTH_RADIUS_METERS * centralAngle;
    }
}
