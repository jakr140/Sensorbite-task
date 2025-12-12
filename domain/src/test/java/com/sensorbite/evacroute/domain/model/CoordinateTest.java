package com.sensorbite.evacroute.domain.model;

import com.sensorbite.evacroute.domain.exception.InvalidCoordinateException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class CoordinateTest {

    @Test
    void shouldCreateValidCoordinate() {
        Coordinate coord = new Coordinate(52.2297, 21.0122);
        assertThat(coord.latitude()).isEqualTo(52.2297);
        assertThat(coord.longitude()).isEqualTo(21.0122);
    }

    @Test
    void shouldRejectInvalidLatitude() {
        assertThatThrownBy(() -> new Coordinate(91.0, 21.0))
                .isInstanceOf(InvalidCoordinateException.class)
                .hasMessageContaining("Latitude must be")
                .hasMessageContaining("91");

        assertThatThrownBy(() -> new Coordinate(-91.0, 21.0))
                .isInstanceOf(InvalidCoordinateException.class);
    }

    @Test
    void shouldRejectInvalidLongitude() {
        assertThatThrownBy(() -> new Coordinate(52.0, 181.0))
                .isInstanceOf(InvalidCoordinateException.class)
                .hasMessageContaining("Longitude must be")
                .hasMessageContaining("181");

        assertThatThrownBy(() -> new Coordinate(52.0, -181.0))
                .isInstanceOf(InvalidCoordinateException.class);
    }

    @Test
    void shouldCalculateDistanceUsingHaversine() {
        Coordinate coord1 = new Coordinate(52.2297, 21.0122);
        Coordinate coord2 = new Coordinate(52.2400, 21.0250);

        double distance = coord1.distanceTo(coord2);

        assertThat(distance).isCloseTo(1523.5, within(100.0));
    }

    @Test
    void shouldReturnZeroDistanceForSamePoint() {
        Coordinate coord = new Coordinate(52.2297, 21.0122);
        assertThat(coord.distanceTo(coord)).isCloseTo(0.0, within(0.1));
    }
}
