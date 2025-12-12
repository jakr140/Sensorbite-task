package com.sensorbite.evacroute.application.service;

import com.sensorbite.evacroute.application.dto.RouteRequest;
import com.sensorbite.evacroute.application.dto.RouteResponse;
import com.sensorbite.evacroute.application.mapper.RouteMapper;
import com.sensorbite.evacroute.domain.model.Coordinate;
import com.sensorbite.evacroute.domain.model.FloodZone;
import com.sensorbite.evacroute.domain.model.Graph;
import com.sensorbite.evacroute.domain.model.RoadNetwork;
import com.sensorbite.evacroute.domain.model.RoadSegment;
import com.sensorbite.evacroute.domain.model.Route;
import com.sensorbite.evacroute.domain.model.RouteMetadata;
import com.sensorbite.evacroute.domain.port.out.FloodZoneRepository;
import com.sensorbite.evacroute.domain.port.out.HazardDetectionPort;
import com.sensorbite.evacroute.domain.port.out.RoadNetworkRepository;
import com.sensorbite.evacroute.domain.service.GraphBuilder;
import com.sensorbite.evacroute.domain.service.RouteCalculationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("RouteApplicationService")
class RouteApplicationServiceTest {

    @Mock
    private RoadNetworkRepository roadNetworkRepository;

    @Mock
    private FloodZoneRepository floodZoneRepository;

    @Mock
    private HazardDetectionPort hazardDetectionPort;

    private RouteApplicationService service;
    private RouteMapper routeMapper;
    private RouteCalculationService routeCalculationService;

    @BeforeEach
    void setUp() {
        routeMapper = Mappers.getMapper(RouteMapper.class);
        routeCalculationService = new RouteCalculationService();
        service = new RouteApplicationService(
                roadNetworkRepository,
                floodZoneRepository,
                hazardDetectionPort,
                routeCalculationService,
                routeMapper
        );
    }

    @Nested
    @DisplayName("Use case orchestration")
    class UseCaseOrchestrationTests {

        @Test
        @DisplayName("should orchestrate route calculation with all dependencies")
        void shouldOrchestrateRouteCalculationWithAllDependencies() {
            RouteRequest request = new RouteRequest("52.0,21.0", "52.2,21.2");
            RoadNetwork network = createTestNetwork();

            when(roadNetworkRepository.load()).thenReturn(network);
            when(floodZoneRepository.loadActiveAt(any(Instant.class))).thenReturn(List.of());
            when(hazardDetectionPort.detectHazardousSegments(anyList(), anyList())).thenReturn(Set.of());

            RouteResponse response = service.calculateRoute(request);

            assertThat(response).isNotNull();
            assertThat(response.type()).isEqualTo("Feature");
            verify(roadNetworkRepository, times(1)).load();
            verify(floodZoneRepository, times(1)).loadActiveAt(any(Instant.class));
        }

        @Test
        @DisplayName("should apply flood zones to network before route calculation")
        void shouldApplyFloodZonesToNetworkBeforeCalculation() {
            RouteRequest request = new RouteRequest("52.0,21.0", "52.1,21.1");
            RoadNetwork network = createTestNetwork();
            FloodZone floodZone = createTestFloodZone();

            when(roadNetworkRepository.load()).thenReturn(network);
            when(floodZoneRepository.loadActiveAt(any(Instant.class))).thenReturn(List.of(floodZone));
            when(hazardDetectionPort.detectHazardousSegments(any(), any()))
                    .thenReturn(Set.of());

            RouteResponse response = service.calculateRoute(request);

            assertThat(response).isNotNull();
            verify(hazardDetectionPort).detectHazardousSegments(any(), any());
        }
    }

    @Nested
    @DisplayName("Distance validation")
    class DistanceValidationTests {

        @Test
        @DisplayName("should accept route within maximum distance")
        void shouldAcceptRouteWithinMaximumDistance() {
            RouteRequest request = new RouteRequest("52.0,21.0", "52.1,21.1");
            RoadNetwork network = createTestNetwork();

            when(roadNetworkRepository.load()).thenReturn(network);
            when(floodZoneRepository.loadActiveAt(any(Instant.class))).thenReturn(List.of());
            when(hazardDetectionPort.detectHazardousSegments(anyList(), anyList())).thenReturn(Set.of());

            RouteResponse response = service.calculateRoute(request);

            assertThat(response).isNotNull();
        }

        @Test
        @DisplayName("should reject route exceeding maximum distance")
        void shouldRejectRouteExceedingMaximumDistance() {
            RouteRequest request = new RouteRequest("52.0,21.0", "60.0,30.0");

            assertThatThrownBy(() -> service.calculateRoute(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Distance between start and end exceeds maximum");
        }

        @Test
        @DisplayName("should validate distance before loading data")
        void shouldValidateDistanceBeforeLoadingData() {
            RouteRequest request = new RouteRequest("0.0,0.0", "89.0,179.0");

            assertThatThrownBy(() -> service.calculateRoute(request))
                    .isInstanceOf(IllegalArgumentException.class);

            verify(roadNetworkRepository, times(0)).load();
        }
    }

    @Nested
    @DisplayName("Coordinate parsing")
    class CoordinateParsingTests {

        @Test
        @DisplayName("should parse valid coordinate strings")
        void shouldParseValidCoordinateStrings() {
            RouteRequest request = new RouteRequest("52.2297,21.0122", "52.2400,21.0250");
            RoadNetwork network = createTestNetwork();

            when(roadNetworkRepository.load()).thenReturn(network);
            when(floodZoneRepository.loadActiveAt(any(Instant.class))).thenReturn(List.of());
            when(hazardDetectionPort.detectHazardousSegments(anyList(), anyList())).thenReturn(Set.of());

            RouteResponse response = service.calculateRoute(request);

            assertThat(response).isNotNull();
        }

        @Test
        @DisplayName("should handle coordinates with whitespace")
        void shouldHandleCoordinatesWithWhitespace() {
            RouteRequest request = new RouteRequest(" 52.0 , 21.0 ", " 52.1 , 21.1 ");
            RoadNetwork network = createTestNetwork();

            when(roadNetworkRepository.load()).thenReturn(network);
            when(floodZoneRepository.loadActiveAt(any(Instant.class))).thenReturn(List.of());
            when(hazardDetectionPort.detectHazardousSegments(anyList(), anyList())).thenReturn(Set.of());

            RouteResponse response = service.calculateRoute(request);

            assertThat(response).isNotNull();
        }
    }

    @Nested
    @DisplayName("Direct use case interface")
    class DirectUseCaseInterfaceTests {

        @Test
        @DisplayName("should calculate route via direct interface")
        void shouldCalculateRouteViaDirectInterface() {
            Coordinate start = new Coordinate(52.0, 21.0);
            Coordinate end = new Coordinate(52.1, 21.1);
            RoadNetwork network = createTestNetwork();

            when(roadNetworkRepository.load()).thenReturn(network);
            when(floodZoneRepository.loadActiveAt(any(Instant.class))).thenReturn(List.of());
            when(hazardDetectionPort.detectHazardousSegments(anyList(), anyList())).thenReturn(Set.of());

            Route route = service.calculateRoute(start, end);

            assertThat(route).isNotNull();
            assertThat(route.getSegments()).isNotEmpty();
        }
    }

    private RoadNetwork createTestNetwork() {
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

    private FloodZone createTestFloodZone() {
        List<List<Coordinate>> rings = List.of(
                List.of(
                        new Coordinate(52.05, 21.05),
                        new Coordinate(52.06, 21.05),
                        new Coordinate(52.06, 21.06),
                        new Coordinate(52.05, 21.05)
                )
        );
        return new FloodZone("zone1", rings, null, null);
    }
}
