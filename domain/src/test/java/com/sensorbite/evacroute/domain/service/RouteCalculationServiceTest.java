package com.sensorbite.evacroute.domain.service;

import com.sensorbite.evacroute.domain.exception.RouteNotFoundException;
import com.sensorbite.evacroute.domain.model.Coordinate;
import com.sensorbite.evacroute.domain.model.Graph;
import com.sensorbite.evacroute.domain.model.RoadNetwork;
import com.sensorbite.evacroute.domain.model.RoadSegment;
import com.sensorbite.evacroute.domain.model.Route;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

@DisplayName("RouteCalculationService")
class RouteCalculationServiceTest {

    private RouteCalculationService service;

    @BeforeEach
    void setUp() {
        service = new RouteCalculationService();
    }

    @Nested
    @DisplayName("Simple route calculation")
    class SimpleRouteTests {

        @Test
        @DisplayName("should calculate route for connected nodes")
        void shouldCalculateRouteForConnectedNodes() {
            RoadNetwork network = createSimpleNetwork();
            Coordinate start = new Coordinate(52.0, 21.0);
            Coordinate end = new Coordinate(52.2, 21.2);

            Route route = service.calculateRoute(network, start, end);

            assertThat(route.getSegments()).hasSize(2);
            assertThat(route.getMetadata().distanceMeters()).isGreaterThan(0);
            assertThat(route.getMetadata().safetyScore()).isEqualTo(1.0);
            assertThat(route.getMetadata().hazardousSegmentsAvoided()).isZero();
        }

        @Test
        @DisplayName("should return empty route when start equals end")
        void shouldReturnEmptyRouteWhenStartEqualsEnd() {
            RoadNetwork network = createSimpleNetwork();
            Coordinate point = new Coordinate(52.0, 21.0);

            Route route = service.calculateRoute(network, point, point);

            assertThat(route.getSegments()).isEmpty();
            assertThat(route.getMetadata().distanceMeters()).isZero();
            assertThat(route.getMetadata().safetyScore()).isEqualTo(1.0);
            assertThat(route.getMetadata().computationTimeMs()).isGreaterThanOrEqualTo(0);
        }

        @Test
        @DisplayName("should find nearest node even for far coordinates")
        void shouldFindNearestNodeEvenForFarCoordinates() {
            RoadNetwork network = createSimpleNetwork();
            Coordinate farStart = new Coordinate(80.0, 80.0);
            Coordinate end = new Coordinate(52.0, 21.0);

            Route route = service.calculateRoute(network, farStart, end);

            assertThat(route).isNotNull();
        }

        @Test
        @DisplayName("should calculate route to nearest node for far endpoint")
        void shouldCalculateRouteToNearestNodeForFarEndpoint() {
            RoadNetwork network = createSimpleNetwork();
            Coordinate start = new Coordinate(52.0, 21.0);
            Coordinate farEnd = new Coordinate(80.0, 80.0);

            Route route = service.calculateRoute(network, start, farEnd);

            assertThat(route).isNotNull();
        }
    }

    @Nested
    @DisplayName("Disconnected graph scenarios")
    class DisconnectedGraphTests {

        @Test
        @DisplayName("should throw exception when no route exists between nodes")
        void shouldThrowExceptionWhenNoRouteExists() {
            RoadNetwork network = createDisconnectedNetwork();
            Coordinate start = new Coordinate(52.0, 21.0);
            Coordinate end = new Coordinate(53.0, 22.0);

            assertThatThrownBy(() -> service.calculateRoute(network, start, end))
                    .isInstanceOf(RouteNotFoundException.class)
                    .hasMessageContaining("No route available between specified points");
        }
    }

    @Nested
    @DisplayName("Hazardous segment handling")
    class HazardousSegmentTests {

        @Test
        @DisplayName("should avoid hazardous segments when alternative exists")
        void shouldAvoidHazardousSegmentsWhenAlternativeExists() {
            RoadNetwork network = createNetworkWithHazard();
            Coordinate start = new Coordinate(52.0, 21.0);
            Coordinate end = new Coordinate(52.2, 21.2);

            Route route = service.calculateRoute(network, start, end);

            assertThat(route.getSegments()).noneMatch(RoadSegment::isHazardous);
            assertThat(route.getMetadata().safetyScore()).isEqualTo(1.0);
        }

        @Test
        @DisplayName("should use hazardous segments when no alternative exists")
        void shouldUseHazardousSegmentsWhenNoAlternativeExists() {
            RoadNetwork network = createNetworkWithOnlyHazardousPath();
            Coordinate start = new Coordinate(52.0, 21.0);
            Coordinate end = new Coordinate(52.1, 21.1);

            Route route = service.calculateRoute(network, start, end);

            assertThat(route.getSegments()).allMatch(RoadSegment::isHazardous);
            assertThat(route.getMetadata().allPathsHazardous()).isTrue();
            assertThat(route.getMetadata().safetyScore()).isZero();
        }

        @Test
        @DisplayName("should calculate safety score correctly for mixed routes")
        void shouldCalculateSafetyScoreForMixedRoutes() {
            RoadNetwork network = createNetworkWithMixedHazards();
            Coordinate start = new Coordinate(52.0, 21.0);
            Coordinate end = new Coordinate(52.3, 21.3);

            Route route = service.calculateRoute(network, start, end);

            long hazardousCount = route.getSegments().stream()
                    .filter(RoadSegment::isHazardous)
                    .count();
            double expectedSafetyScore = 1.0 - ((double) hazardousCount / route.getSegments().size());

            assertThat(route.getMetadata().safetyScore()).isCloseTo(expectedSafetyScore, within(0.01));
            assertThat(route.getMetadata().hazardousSegmentsAvoided()).isEqualTo((int) hazardousCount);
        }
    }

    @Nested
    @DisplayName("Parametrized graph scenarios")
    class ParametrizedTests {

        @ParameterizedTest
        @MethodSource("com.sensorbite.evacroute.domain.service.RouteCalculationServiceTest#graphScenarios")
        @DisplayName("should handle different graph scenarios")
        void shouldHandleDifferentGraphScenarios(GraphScenario scenario) {
            if (scenario.shouldThrowException()) {
                assertThatThrownBy(() -> service.calculateRoute(
                        scenario.network(),
                        scenario.start(),
                        scenario.end()
                )).isInstanceOf(RouteNotFoundException.class);
            } else {
                Route route = service.calculateRoute(
                        scenario.network(),
                        scenario.start(),
                        scenario.end()
                );

                assertThat(route).isNotNull();
                assertThat(route.getMetadata()).isNotNull();
                scenario.verify(route);
            }
        }
    }

    @Nested
    @DisplayName("Route metadata")
    class MetadataTests {

        @Test
        @DisplayName("should include computation time")
        void shouldIncludeComputationTime() {
            RoadNetwork network = createSimpleNetwork();
            Coordinate start = new Coordinate(52.0, 21.0);
            Coordinate end = new Coordinate(52.2, 21.2);

            Route route = service.calculateRoute(network, start, end);

            assertThat(route.getMetadata().computationTimeMs()).isGreaterThanOrEqualTo(0);
        }

        @Test
        @DisplayName("should include timestamp")
        void shouldIncludeTimestamp() {
            RoadNetwork network = createSimpleNetwork();
            Coordinate start = new Coordinate(52.0, 21.0);
            Coordinate end = new Coordinate(52.2, 21.2);

            Route route = service.calculateRoute(network, start, end);

            assertThat(route.getMetadata().timestamp()).isNotNull();
        }

        @Test
        @DisplayName("should calculate total distance correctly")
        void shouldCalculateTotalDistanceCorrectly() {
            RoadNetwork network = createSimpleNetwork();
            Coordinate start = new Coordinate(52.0, 21.0);
            Coordinate end = new Coordinate(52.2, 21.2);

            Route route = service.calculateRoute(network, start, end);

            double expectedDistance = route.getSegments().stream()
                    .mapToDouble(RoadSegment::getLengthMeters)
                    .sum();

            assertThat(route.getMetadata().distanceMeters()).isCloseTo(expectedDistance, within(0.01));
        }
    }

    static Stream<GraphScenario> graphScenarios() {
        return Stream.of(
                new GraphScenario(
                        "Simple path",
                        createSimpleNetwork(),
                        new Coordinate(52.0, 21.0),
                        new Coordinate(52.2, 21.2),
                        false,
                        route -> assertThat(route.getSegments()).isNotEmpty()
                ),
                new GraphScenario(
                        "All hazardous path",
                        createNetworkWithOnlyHazardousPath(),
                        new Coordinate(52.0, 21.0),
                        new Coordinate(52.1, 21.1),
                        false,
                        route -> assertThat(route.getMetadata().allPathsHazardous()).isTrue()
                ),
                new GraphScenario(
                        "Disconnected graph",
                        createDisconnectedNetwork(),
                        new Coordinate(52.0, 21.0),
                        new Coordinate(53.0, 22.0),
                        true,
                        route -> {}
                ),
                new GraphScenario(
                        "Single segment",
                        createSingleSegmentNetwork(),
                        new Coordinate(52.0, 21.0),
                        new Coordinate(52.1, 21.1),
                        false,
                        route -> assertThat(route.getSegments()).hasSize(1)
                )
        );
    }

    private static RoadNetwork createSimpleNetwork() {
        List<RoadSegment> segments = List.of(
                new RoadSegment("seg1", List.of(
                        new Coordinate(52.0, 21.0),
                        new Coordinate(52.1, 21.1)
                ), false),
                new RoadSegment("seg2", List.of(
                        new Coordinate(52.1, 21.1),
                        new Coordinate(52.2, 21.2)
                ), false)
        );
        Graph graph = new GraphBuilder().buildGraph(segments);
        return new RoadNetwork(segments, graph);
    }

    private static RoadNetwork createDisconnectedNetwork() {
        List<RoadSegment> segments = List.of(
                new RoadSegment("seg1", List.of(
                        new Coordinate(52.0, 21.0),
                        new Coordinate(52.1, 21.1)
                ), false),
                new RoadSegment("seg2", List.of(
                        new Coordinate(53.0, 22.0),
                        new Coordinate(53.1, 22.1)
                ), false)
        );
        Graph graph = new GraphBuilder().buildGraph(segments);
        return new RoadNetwork(segments, graph);
    }

    private static RoadNetwork createNetworkWithHazard() {
        List<RoadSegment> segments = List.of(
                new RoadSegment("seg1", List.of(
                        new Coordinate(52.0, 21.0),
                        new Coordinate(52.1, 21.1)
                ), false, true),
                new RoadSegment("seg2", List.of(
                        new Coordinate(52.0, 21.0),
                        new Coordinate(52.05, 21.05)
                ), false, false),
                new RoadSegment("seg3", List.of(
                        new Coordinate(52.05, 21.05),
                        new Coordinate(52.15, 21.15)
                ), false, false),
                new RoadSegment("seg4", List.of(
                        new Coordinate(52.15, 21.15),
                        new Coordinate(52.2, 21.2)
                ), false, false),
                new RoadSegment("seg5", List.of(
                        new Coordinate(52.1, 21.1),
                        new Coordinate(52.2, 21.2)
                ), false, false)
        );
        Graph graph = new GraphBuilder().buildGraph(segments);
        return new RoadNetwork(segments, graph);
    }

    private static RoadNetwork createNetworkWithOnlyHazardousPath() {
        List<RoadSegment> segments = List.of(
                new RoadSegment("seg1", List.of(
                        new Coordinate(52.0, 21.0),
                        new Coordinate(52.1, 21.1)
                ), false, true)
        );
        Graph graph = new GraphBuilder().buildGraph(segments);
        return new RoadNetwork(segments, graph);
    }

    private static RoadNetwork createNetworkWithMixedHazards() {
        List<RoadSegment> segments = List.of(
                new RoadSegment("seg1", List.of(
                        new Coordinate(52.0, 21.0),
                        new Coordinate(52.1, 21.1)
                ), false, false),
                new RoadSegment("seg2", List.of(
                        new Coordinate(52.1, 21.1),
                        new Coordinate(52.2, 21.2)
                ), false, true),
                new RoadSegment("seg3", List.of(
                        new Coordinate(52.2, 21.2),
                        new Coordinate(52.3, 21.3)
                ), false, false)
        );
        Graph graph = new GraphBuilder().buildGraph(segments);
        return new RoadNetwork(segments, graph);
    }

    private static RoadNetwork createSingleSegmentNetwork() {
        List<RoadSegment> segments = List.of(
                new RoadSegment("seg1", List.of(
                        new Coordinate(52.0, 21.0),
                        new Coordinate(52.1, 21.1)
                ), false)
        );
        Graph graph = new GraphBuilder().buildGraph(segments);
        return new RoadNetwork(segments, graph);
    }

    static class GraphScenario {
        private final String name;
        private final RoadNetwork network;
        private final Coordinate start;
        private final Coordinate end;
        private final boolean shouldThrowException;
        private final java.util.function.Consumer<Route> verifier;

        public GraphScenario(String name, RoadNetwork network, Coordinate start, Coordinate end,
                           boolean shouldThrowException, java.util.function.Consumer<Route> verifier) {
            this.name = name;
            this.network = network;
            this.start = start;
            this.end = end;
            this.shouldThrowException = shouldThrowException;
            this.verifier = verifier;
        }

        public String name() { return name; }
        public RoadNetwork network() { return network; }
        public Coordinate start() { return start; }
        public Coordinate end() { return end; }
        public boolean shouldThrowException() { return shouldThrowException; }
        public void verify(Route route) { verifier.accept(route); }

        @Override
        public String toString() {
            return name;
        }
    }
}
