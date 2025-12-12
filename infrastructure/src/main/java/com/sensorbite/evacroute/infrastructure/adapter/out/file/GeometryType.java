package com.sensorbite.evacroute.infrastructure.adapter.out.file;

/**
 * Standard GeoJSON geometry types used in road network and flood zone data.
 *
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc7946#section-3.1">GeoJSON Geometry Objects</a>
 */
public enum GeometryType {
    LINE_STRING("LineString"),
    MULTI_LINE_STRING("MultiLineString"),
    POLYGON("Polygon"),
    MULTI_POLYGON("MultiPolygon"),
    POINT("Point"),
    MULTI_POINT("MultiPoint");

    private final String geoJsonName;

    GeometryType(String geoJsonName) {
        this.geoJsonName = geoJsonName;
    }

    public String getGeoJsonName() {
        return geoJsonName;
    }

    /**
     * Parse geometry type from GeoJSON string.
     *
     * @param geoJsonName the GeoJSON geometry type string
     * @return matching GeometryType, or null if not found
     */
    public static GeometryType fromGeoJsonName(String geoJsonName) {
        for (GeometryType type : values()) {
            if (type.geoJsonName.equals(geoJsonName)) {
                return type;
            }
        }
        return null;
    }

    public boolean matches(String geoJsonName) {
        return this.geoJsonName.equals(geoJsonName);
    }
}
