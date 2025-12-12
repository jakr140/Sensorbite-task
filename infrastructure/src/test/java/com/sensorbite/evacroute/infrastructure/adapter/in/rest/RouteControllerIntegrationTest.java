package com.sensorbite.evacroute.infrastructure.adapter.in.rest;

import com.sensorbite.evacroute.application.service.RouteApplicationService;
import com.sensorbite.evacroute.domain.model.Coordinate;
import com.sensorbite.evacroute.domain.model.FloodZone;
import com.sensorbite.evacroute.domain.model.Graph;
import com.sensorbite.evacroute.domain.model.RoadNetwork;
import com.sensorbite.evacroute.domain.model.RoadSegment;
import com.sensorbite.evacroute.domain.port.out.FloodZoneRepository;
import com.sensorbite.evacroute.domain.port.out.HazardDetectionPort;
import com.sensorbite.evacroute.domain.port.out.RoadNetworkRepository;
import com.sensorbite.evacroute.domain.service.GraphBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("RouteController Integration Tests")
class RouteControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RoadNetworkRepository roadNetworkRepository;

    @MockBean
    private FloodZoneRepository floodZoneRepository;

    @MockBean
    private HazardDetectionPort hazardDetectionPort;

    @BeforeEach
    void setUp() {
        when(floodZoneRepository.loadActiveAt(any(Instant.class))).thenReturn(List.of());
        when(hazardDetectionPort.detectHazardousSegments(anyList(), anyList())).thenReturn(Set.of());
    }

    @Nested
    @DisplayName("Successful route calculation")
    class SuccessfulRouteTests {

        @Test
        @DisplayName("should return route for valid coordinates")
        void shouldReturnRouteForValidCoordinates() throws Exception {
            when(roadNetworkRepository.load()).thenReturn(createTestNetwork());

            mockMvc.perform(get("/api/evac/route")
                            .param("start", "52.0,21.0")
                            .param("end", "52.2,21.2")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.type", is("Feature")))
                    .andExpect(jsonPath("$.geometry.type", is("LineString")))
                    .andExpect(jsonPath("$.geometry.coordinates").isArray())
                    .andExpect(jsonPath("$.properties.distanceMeters", greaterThan(0.0)))
                    .andExpect(jsonPath("$.properties.safetyScore", is(1.0)))
                    .andExpect(jsonPath("$.properties.timestamp", notNullValue()));
        }

        @Test
        @DisplayName("should handle coordinates with whitespace")
        void shouldHandleCoordinatesWithWhitespace() throws Exception {
            when(roadNetworkRepository.load()).thenReturn(createTestNetwork());

            mockMvc.perform(get("/api/evac/route")
                            .param("start", " 52.0 , 21.0 ")
                            .param("end", " 52.1 , 21.1 ")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.type", is("Feature")));
        }

        @Test
        @DisplayName("should return GeoJSON with correct coordinate order (lon,lat)")
        void shouldReturnGeoJsonWithCorrectCoordinateOrder() throws Exception {
            when(roadNetworkRepository.load()).thenReturn(createTestNetwork());

            mockMvc.perform(get("/api/evac/route")
                            .param("start", "52.0,21.0")
                            .param("end", "52.1,21.1")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.geometry.coordinates[0][0]", is(21.0)))
                    .andExpect(jsonPath("$.geometry.coordinates[0][1]", is(52.0)));
        }
    }

    @Nested
    @DisplayName("Parameter validation")
    class ParameterValidationTests {

        @Test
        @DisplayName("should reject request without start parameter")
        void shouldRejectRequestWithoutStartParameter() throws Exception {
            mockMvc.perform(get("/api/evac/route")
                            .param("end", "52.1,21.1")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should reject request without end parameter")
        void shouldRejectRequestWithoutEndParameter() throws Exception {
            mockMvc.perform(get("/api/evac/route")
                            .param("start", "52.0,21.0")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should reject blank start coordinate")
        void shouldRejectBlankStartCoordinate() throws Exception {
            mockMvc.perform(get("/api/evac/route")
                            .param("start", "  ")
                            .param("end", "52.1,21.1")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorType", is("VALIDATION_ERROR")));
        }

        @Test
        @DisplayName("should reject invalid coordinate format")
        void shouldRejectInvalidCoordinateFormat() throws Exception {
            mockMvc.perform(get("/api/evac/route")
                            .param("start", "invalid")
                            .param("end", "52.1,21.1")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorType", is("VALIDATION_ERROR")))
                    .andExpect(jsonPath("$.message", containsString("Invalid coordinate format")));
        }

        @Test
        @DisplayName("should reject coordinates without comma")
        void shouldRejectCoordinatesWithoutComma() throws Exception {
            mockMvc.perform(get("/api/evac/route")
                            .param("start", "52.0 21.0")
                            .param("end", "52.1,21.1")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorType", is("VALIDATION_ERROR")));
        }

        @Test
        @DisplayName("should reject out of range latitude")
        void shouldRejectOutOfRangeLatitude() throws Exception {
            mockMvc.perform(get("/api/evac/route")
                            .param("start", "91.0,21.0")
                            .param("end", "52.1,21.1")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorType", is("VALIDATION_ERROR")))
                    .andExpect(jsonPath("$.message", containsString("Latitude")));
        }

        @Test
        @DisplayName("should reject out of range longitude")
        void shouldRejectOutOfRangeLongitude() throws Exception {
            mockMvc.perform(get("/api/evac/route")
                            .param("start", "52.0,181.0")
                            .param("end", "52.1,21.1")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorType", is("VALIDATION_ERROR")))
                    .andExpect(jsonPath("$.message", containsString("Longitude")));
        }
    }

    @Nested
    @DisplayName("Error scenarios")
    class ErrorScenarioTests {

        @Test
        @DisplayName("should return 404 when no route found")
        void shouldReturn404WhenNoRouteFound() throws Exception {
            when(roadNetworkRepository.load()).thenReturn(createDisconnectedNetwork());

            mockMvc.perform(get("/api/evac/route")
                            .param("start", "52.0,21.0")
                            .param("end", "53.0,22.0")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errorType", is("ROUTE_NOT_FOUND")))
                    .andExpect(jsonPath("$.message", containsString("No route available")))
                    .andExpect(jsonPath("$.requestId", notNullValue()));
        }

        @Test
        @DisplayName("should return 400 when distance exceeds maximum")
        void shouldReturn400WhenDistanceExceedsMaximum() throws Exception {
            mockMvc.perform(get("/api/evac/route")
                            .param("start", "0.0,0.0")
                            .param("end", "89.0,179.0")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorType", is("VALIDATION_ERROR")))
                    .andExpect(jsonPath("$.message", containsString("exceeds maximum")));
        }
    }

    @Nested
    @DisplayName("Flood zone handling")
    class FloodZoneHandlingTests {

        @Test
        @DisplayName("should avoid hazardous segments when flood zones present")
        void shouldAvoidHazardousSegmentsWhenFloodZonesPresent() throws Exception {
            RoadNetwork network = createNetworkWithAlternativePaths();
            FloodZone zone = createTestFloodZone();

            when(roadNetworkRepository.load()).thenReturn(network);
            when(floodZoneRepository.loadActiveAt(any(Instant.class))).thenReturn(List.of(zone));
            when(hazardDetectionPort.detectHazardousSegments(anyList(), anyList()))
                    .thenReturn(Set.of("seg1"));

            mockMvc.perform(get("/api/evac/route")
                            .param("start", "52.0,21.0")
                            .param("end", "52.2,21.2")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.properties.safetyScore").exists());
        }
    }

    @Nested
    @DisplayName("Content negotiation")
    class ContentNegotiationTests {

        @Test
        @DisplayName("should return JSON by default")
        void shouldReturnJsonByDefault() throws Exception {
            when(roadNetworkRepository.load()).thenReturn(createTestNetwork());

            mockMvc.perform(get("/api/evac/route")
                            .param("start", "52.0,21.0")
                            .param("end", "52.1,21.1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.type", is("Feature")));
        }

        @Test
        @DisplayName("should accept application/json")
        void shouldAcceptApplicationJson() throws Exception {
            when(roadNetworkRepository.load()).thenReturn(createTestNetwork());

            mockMvc.perform(get("/api/evac/route")
                            .param("start", "52.0,21.0")
                            .param("end", "52.1,21.1")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
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

    private RoadNetwork createDisconnectedNetwork() {
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

    private RoadNetwork createNetworkWithAlternativePaths() {
        List<RoadSegment> segments = List.of(
                new RoadSegment("seg1", List.of(
                        new Coordinate(52.0, 21.0),
                        new Coordinate(52.1, 21.1)
                ), false),
                new RoadSegment("seg2", List.of(
                        new Coordinate(52.0, 21.0),
                        new Coordinate(52.05, 21.05)
                ), false),
                new RoadSegment("seg3", List.of(
                        new Coordinate(52.05, 21.05),
                        new Coordinate(52.15, 21.15)
                ), false),
                new RoadSegment("seg4", List.of(
                        new Coordinate(52.15, 21.15),
                        new Coordinate(52.2, 21.2)
                ), false),
                new RoadSegment("seg5", List.of(
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
