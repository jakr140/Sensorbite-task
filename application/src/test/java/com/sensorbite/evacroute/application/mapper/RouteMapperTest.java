package com.sensorbite.evacroute.application.mapper;

import com.sensorbite.evacroute.application.dto.RouteResponse;
import com.sensorbite.evacroute.domain.exception.InvalidCoordinateException;
import com.sensorbite.evacroute.domain.model.Coordinate;
import com.sensorbite.evacroute.domain.model.RoadSegment;
import com.sensorbite.evacroute.domain.model.Route;
import com.sensorbite.evacroute.domain.model.RouteMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mapstruct.factory.Mappers;

import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

@DisplayName("RouteMapper")
class RouteMapperTest {

    private RouteMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = Mappers.getMapper(RouteMapper.class);
    }

    @Nested
    @DisplayName("Coordinate parsing")
    class CoordinateParsingTests {

        @Test
        @DisplayName("should parse valid coordinate string")
        void shouldParseValidCoordinateString() {
            Coordinate coord = mapper.parseCoordinate("52.2297,21.0122");

            assertThat(coord.latitude()).isCloseTo(52.2297, within(0.000001));
            assertThat(coord.longitude()).isCloseTo(21.0122, within(0.000001));
        }

        @Test
        @DisplayName("should parse coordinate with whitespace")
        void shouldParseCoordinateWithWhitespace() {
            Coordinate coord = mapper.parseCoordinate(" 52.2297 , 21.0122 ");

            assertThat(coord.latitude()).isCloseTo(52.2297, within(0.000001));
            assertThat(coord.longitude()).isCloseTo(21.0122, within(0.000001));
        }

        @Test
        @DisplayName("should parse negative coordinates")
        void shouldParseNegativeCoordinates() {
            Coordinate coord = mapper.parseCoordinate("-52.2297,-21.0122");

            assertThat(coord.latitude()).isCloseTo(-52.2297, within(0.000001));
            assertThat(coord.longitude()).isCloseTo(-21.0122, within(0.000001));
        }

        @Test
        @DisplayName("should round to 6 decimal places")
        void shouldRoundTo6DecimalPlaces() {
            Coordinate coord = mapper.parseCoordinate("52.123456789,21.987654321");

            assertThat(coord.latitude()).isEqualTo(52.123457);
            assertThat(coord.longitude()).isEqualTo(21.987654);
        }

        @Test
        @DisplayName("should reject null coordinate string")
        void shouldRejectNullCoordinateString() {
            assertThatThrownBy(() -> mapper.parseCoordinate(null))
                    .isInstanceOf(InvalidCoordinateException.class)
                    .hasMessageContaining("cannot be null or blank");
        }

        @Test
        @DisplayName("should reject blank coordinate string")
        void shouldRejectBlankCoordinateString() {
            assertThatThrownBy(() -> mapper.parseCoordinate("  "))
                    .isInstanceOf(InvalidCoordinateException.class)
                    .hasMessageContaining("cannot be null or blank");
        }

        @Test
        @DisplayName("should reject invalid format - missing comma")
        void shouldRejectInvalidFormatMissingComma() {
            assertThatThrownBy(() -> mapper.parseCoordinate("52.2297 21.0122"))
                    .isInstanceOf(InvalidCoordinateException.class)
                    .hasMessageContaining("Invalid coordinate format");
        }

        @Test
        @DisplayName("should reject invalid format - too many parts")
        void shouldRejectInvalidFormatTooManyParts() {
            assertThatThrownBy(() -> mapper.parseCoordinate("52.2297,21.0122,100"))
                    .isInstanceOf(InvalidCoordinateException.class)
                    .hasMessageContaining("Invalid coordinate format");
        }

        @Test
        @DisplayName("should reject invalid format - non-numeric")
        void shouldRejectInvalidFormatNonNumeric() {
            assertThatThrownBy(() -> mapper.parseCoordinate("abc,def"))
                    .isInstanceOf(InvalidCoordinateException.class)
                    .hasMessageContaining("Invalid coordinate numbers");
        }

        @ParameterizedTest
        @MethodSource("invalidCoordinateStrings")
        @DisplayName("should reject various invalid coordinate strings")
        void shouldRejectVariousInvalidCoordinateStrings(String coordString) {
            assertThatThrownBy(() -> mapper.parseCoordinate(coordString))
                    .isInstanceOf(InvalidCoordinateException.class);
        }

        static Stream<String> invalidCoordinateStrings() {
            return Stream.of(
                    "",
                    "   ",
                    ",",
                    "52.2297,",
                    ",21.0122",
                    "abc,123",
                    "123,abc",
                    "52.2297,21.0122,extra"
            );
        }
    }

    @Nested
    @DisplayName("Route to Response conversion")
    class RouteToResponseTests {

        @Test
        @DisplayName("should convert route to GeoJSON response")
        void shouldConvertRouteToGeoJsonResponse() {
            Route route = createTestRoute();

            RouteResponse response = mapper.toResponse(route);

            assertThat(response.type()).isEqualTo("Feature");
            assertThat(response.geometry().type()).isEqualTo("LineString");
            assertThat(response.properties()).isNotNull();
        }

        @Test
        @DisplayName("should format coordinates as lon,lat in GeoJSON")
        void shouldFormatCoordinatesAsLonLatInGeoJson() {
            Route route = createTestRoute();

            RouteResponse response = mapper.toResponse(route);

            List<List<Double>> coordinates = response.geometry().coordinates();
            assertThat(coordinates).hasSize(2);
            assertThat(coordinates.get(0)).containsExactly(21.0, 52.0);
            assertThat(coordinates.get(1)).containsExactly(21.1, 52.1);
        }

        @Test
        @DisplayName("should include all metadata in properties")
        void shouldIncludeAllMetadataInProperties() {
            Route route = createTestRoute();

            RouteResponse response = mapper.toResponse(route);

            assertThat(response.properties().distanceMeters()).isEqualTo(100.0);
            assertThat(response.properties().computationTimeMs()).isEqualTo(50L);
            assertThat(response.properties().hazardousSegmentsAvoided()).isEqualTo(2);
            assertThat(response.properties().safetyScore()).isEqualTo(0.95);
            assertThat(response.properties().allPathsHazardous()).isFalse();
            assertThat(response.properties().timestamp()).isNotNull();
        }

        @Test
        @DisplayName("should format timestamp as ISO-8601 string")
        void shouldFormatTimestampAsIso8601String() {
            Instant timestamp = Instant.parse("2024-01-15T10:30:00Z");
            Route route = createTestRouteWithTimestamp(timestamp);

            RouteResponse response = mapper.toResponse(route);

            assertThat(response.properties().timestamp()).isEqualTo("2024-01-15T10:30:00Z");
        }

        @Test
        @DisplayName("should handle route with multiple segments")
        void shouldHandleRouteWithMultipleSegments() {
            List<RoadSegment> segments = List.of(
                    new RoadSegment("seg1", List.of(
                            new Coordinate(52.0, 21.0),
                            new Coordinate(52.05, 21.05),
                            new Coordinate(52.1, 21.1)
                    ), false),
                    new RoadSegment("seg2", List.of(
                            new Coordinate(52.1, 21.1),
                            new Coordinate(52.15, 21.15),
                            new Coordinate(52.2, 21.2)
                    ), false)
            );
            RouteMetadata metadata = new RouteMetadata(200.0, 75L, 0, 1.0, Instant.now(), false);
            Route route = new Route(segments, metadata);

            RouteResponse response = mapper.toResponse(route);

            assertThat(response.geometry().coordinates()).hasSize(6);
        }
    }

    private Route createTestRoute() {
        List<RoadSegment> segments = List.of(
                new RoadSegment("seg1", List.of(
                        new Coordinate(52.0, 21.0),
                        new Coordinate(52.1, 21.1)
                ), false)
        );
        RouteMetadata metadata = new RouteMetadata(100.0, 50L, 2, 0.95, Instant.now(), false);
        return new Route(segments, metadata);
    }

    private Route createTestRouteWithTimestamp(Instant timestamp) {
        List<RoadSegment> segments = List.of(
                new RoadSegment("seg1", List.of(
                        new Coordinate(52.0, 21.0),
                        new Coordinate(52.1, 21.1)
                ), false)
        );
        RouteMetadata metadata = new RouteMetadata(100.0, 50L, 2, 0.95, timestamp, false);
        return new Route(segments, metadata);
    }
}
