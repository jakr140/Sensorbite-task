package com.sensorbite.evacroute.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("FloodZone")
class FloodZoneTest {

    @Nested
    @DisplayName("Validation tests")
    class ValidationTests {

        @Test
        @DisplayName("should create valid flood zone")
        void shouldCreateValidFloodZone() {
            List<List<Coordinate>> rings = List.of(
                    List.of(
                            new Coordinate(52.0, 21.0),
                            new Coordinate(52.1, 21.0),
                            new Coordinate(52.1, 21.1),
                            new Coordinate(52.0, 21.0)
                    )
            );

            FloodZone zone = new FloodZone("zone1", rings, null, null);

            assertThat(zone.id()).isEqualTo("zone1");
            assertThat(zone.polygonRings()).hasSize(1);
        }

        @Test
        @DisplayName("should reject null ID")
        void shouldRejectNullId() {
            List<List<Coordinate>> rings = List.of(
                    List.of(
                            new Coordinate(52.0, 21.0),
                            new Coordinate(52.1, 21.0),
                            new Coordinate(52.1, 21.1),
                            new Coordinate(52.0, 21.0)
                    )
            );

            assertThatThrownBy(() -> new FloodZone(null, rings, null, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("ID cannot be null or blank");
        }

        @Test
        @DisplayName("should reject blank ID")
        void shouldRejectBlankId() {
            List<List<Coordinate>> rings = List.of(
                    List.of(
                            new Coordinate(52.0, 21.0),
                            new Coordinate(52.1, 21.0),
                            new Coordinate(52.1, 21.1),
                            new Coordinate(52.0, 21.0)
                    )
            );

            assertThatThrownBy(() -> new FloodZone("  ", rings, null, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("ID cannot be null or blank");
        }

        @Test
        @DisplayName("should reject null polygon rings")
        void shouldRejectNullPolygonRings() {
            assertThatThrownBy(() -> new FloodZone("zone1", null, null, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must have at least one polygon ring");
        }

        @Test
        @DisplayName("should reject empty polygon rings")
        void shouldRejectEmptyPolygonRings() {
            assertThatThrownBy(() -> new FloodZone("zone1", List.of(), null, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must have at least one polygon ring");
        }

        @Test
        @DisplayName("should reject ring with less than 3 points")
        void shouldRejectRingWithLessThan3Points() {
            List<List<Coordinate>> rings = List.of(
                    List.of(
                            new Coordinate(52.0, 21.0),
                            new Coordinate(52.1, 21.0)
                    )
            );

            assertThatThrownBy(() -> new FloodZone("zone1", rings, null, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must have at least 3 points");
        }

        @Test
        @DisplayName("should reject null ring")
        void shouldRejectNullRing() {
            List<List<Coordinate>> rings = java.util.Arrays.asList((List<Coordinate>) null);

            assertThatThrownBy(() -> new FloodZone("zone1", rings, null, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must have at least 3 points");
        }
    }

    @Nested
    @DisplayName("Temporal validity tests")
    class TemporalValidityTests {

        @Test
        @DisplayName("should be valid when no temporal bounds set")
        void shouldBeValidWhenNoTemporalBounds() {
            FloodZone zone = createZone(null, null);

            assertThat(zone.isValidAt(Instant.now())).isTrue();
            assertThat(zone.isValidAt(Instant.now().minus(1, ChronoUnit.DAYS))).isTrue();
            assertThat(zone.isValidAt(Instant.now().plus(1, ChronoUnit.DAYS))).isTrue();
        }

        @Test
        @DisplayName("should be valid within temporal bounds")
        void shouldBeValidWithinTemporalBounds() {
            Instant validFrom = Instant.now().minus(1, ChronoUnit.HOURS);
            Instant validUntil = Instant.now().plus(1, ChronoUnit.HOURS);
            FloodZone zone = createZone(validFrom, validUntil);

            assertThat(zone.isValidAt(Instant.now())).isTrue();
        }

        @Test
        @DisplayName("should be invalid before validFrom")
        void shouldBeInvalidBeforeValidFrom() {
            Instant validFrom = Instant.now().plus(1, ChronoUnit.HOURS);
            FloodZone zone = createZone(validFrom, null);

            assertThat(zone.isValidAt(Instant.now())).isFalse();
        }

        @Test
        @DisplayName("should be invalid after validUntil")
        void shouldBeInvalidAfterValidUntil() {
            Instant validUntil = Instant.now().minus(1, ChronoUnit.HOURS);
            FloodZone zone = createZone(null, validUntil);

            assertThat(zone.isValidAt(Instant.now())).isFalse();
        }

        @Test
        @DisplayName("should be valid at exact validFrom time")
        void shouldBeValidAtExactValidFromTime() {
            Instant validFrom = Instant.parse("2024-01-01T00:00:00Z");
            FloodZone zone = createZone(validFrom, null);

            assertThat(zone.isValidAt(validFrom)).isTrue();
        }

        @Test
        @DisplayName("should be valid at exact validUntil time")
        void shouldBeValidAtExactValidUntilTime() {
            Instant validUntil = Instant.parse("2024-01-02T00:00:00Z");
            FloodZone zone = createZone(null, validUntil);

            assertThat(zone.isValidAt(validUntil)).isTrue();
        }

        @Test
        @DisplayName("should handle only validFrom bound")
        void shouldHandleOnlyValidFromBound() {
            Instant validFrom = Instant.now().minus(1, ChronoUnit.HOURS);
            FloodZone zone = createZone(validFrom, null);

            assertThat(zone.isValidAt(Instant.now())).isTrue();
            assertThat(zone.isValidAt(validFrom.minus(1, ChronoUnit.SECONDS))).isFalse();
        }

        @Test
        @DisplayName("should handle only validUntil bound")
        void shouldHandleOnlyValidUntilBound() {
            Instant validUntil = Instant.now().plus(1, ChronoUnit.HOURS);
            FloodZone zone = createZone(null, validUntil);

            assertThat(zone.isValidAt(Instant.now())).isTrue();
            assertThat(zone.isValidAt(validUntil.plus(1, ChronoUnit.SECONDS))).isFalse();
        }
    }

    @Nested
    @DisplayName("Immutability tests")
    class ImmutabilityTests {

        @Test
        @DisplayName("should return defensive copy of polygon rings")
        void shouldReturnDefensiveCopyOfPolygonRings() {
            List<List<Coordinate>> rings = List.of(
                    List.of(
                            new Coordinate(52.0, 21.0),
                            new Coordinate(52.1, 21.0),
                            new Coordinate(52.1, 21.1),
                            new Coordinate(52.0, 21.0)
                    )
            );

            FloodZone zone = new FloodZone("zone1", rings, null, null);

            assertThat(zone.polygonRings()).isNotSameAs(rings);
        }

        @Test
        @DisplayName("should create flood zone with multiple rings")
        void shouldCreateFloodZoneWithMultipleRings() {
            List<List<Coordinate>> rings = List.of(
                    List.of(
                            new Coordinate(52.0, 21.0),
                            new Coordinate(52.2, 21.0),
                            new Coordinate(52.2, 21.2),
                            new Coordinate(52.0, 21.0)
                    ),
                    List.of(
                            new Coordinate(52.05, 21.05),
                            new Coordinate(52.15, 21.05),
                            new Coordinate(52.15, 21.15),
                            new Coordinate(52.05, 21.05)
                    )
            );

            FloodZone zone = new FloodZone("zone1", rings, null, null);

            assertThat(zone.polygonRings()).hasSize(2);
            assertThat(zone.polygonRings().get(0)).hasSize(4);
            assertThat(zone.polygonRings().get(1)).hasSize(4);
        }
    }

    private FloodZone createZone(Instant validFrom, Instant validUntil) {
        List<List<Coordinate>> rings = List.of(
                List.of(
                        new Coordinate(52.0, 21.0),
                        new Coordinate(52.1, 21.0),
                        new Coordinate(52.1, 21.1),
                        new Coordinate(52.0, 21.0)
                )
        );
        return new FloodZone("zone1", rings, validFrom, validUntil);
    }
}
