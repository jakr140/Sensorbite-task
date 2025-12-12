package com.sensorbite.evacroute.infrastructure.adapter.out.file;

/**
 * GeoJSON property keys and values used in road network and flood zone data.
 *
 * <p>These constants represent the expected schema from OpenStreetMap-derived
 * GeoJSON exports and flood zone data formats.</p>
 */
public final class GeoJsonProperty {

    private GeoJsonProperty() {
        throw new UnsupportedOperationException("Utility class");
    }

    // Road network properties
    public static final String ONEWAY = "oneway";
    public static final String ROAD_TYPE = "highway";
    public static final String MAX_SPEED = "maxspeed";

    // Flood zone properties
    public static final String VALID_FROM = "validFrom";
    public static final String VALID_UNTIL = "validUntil";
    public static final String HAZARD_LEVEL = "hazardLevel";

    // Common boolean value representations
    public static final String BOOLEAN_YES = "yes";
    public static final String BOOLEAN_TRUE = "true";
    public static final String BOOLEAN_ONE = "1";
    public static final String BOOLEAN_NO = "no";
    public static final String BOOLEAN_FALSE = "false";
    public static final String BOOLEAN_ZERO = "0";

    /**
     * Parse boolean value from GeoJSON property.
     * Recognizes: "yes", "true", "1" as true; "no", "false", "0" as false.
     *
     * @param value the property value
     * @return true if value represents true, false otherwise
     */
    public static boolean parseBoolean(String value) {
        if (value == null) {
            return false;
        }
        return BOOLEAN_YES.equalsIgnoreCase(value) ||
               BOOLEAN_TRUE.equalsIgnoreCase(value) ||
               BOOLEAN_ONE.equals(value);
    }
}
