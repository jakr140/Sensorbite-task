package com.sensorbite.evacroute.infrastructure.adapter.in.rest;

import com.sensorbite.evacroute.application.dto.RouteRequest;
import com.sensorbite.evacroute.application.dto.RouteResponse;
import com.sensorbite.evacroute.application.service.RouteApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/evac")
@RequiredArgsConstructor
@Validated
@Tag(name = "Evacuation Routes", description = "Calculate safe evacuation routes avoiding flood zones")
public class RouteController {

    private final RouteApplicationService routeApplicationService;

    @GetMapping("/route")
    @Operation(
        summary = "Calculate evacuation route",
        description = "Calculates the safest route between two points avoiding flood zones. " +
                     "Coordinates should be in lat,lon format (e.g., 52.2297,21.0122). " +
                     "Returns GeoJSON Feature with LineString geometry."
    )
    @ApiResponse(
        responseCode = "200",
        description = "Route successfully calculated",
        content = @Content(schema = @Schema(implementation = RouteResponse.class))
    )
    @ApiResponse(responseCode = "400", description = "Invalid coordinates")
    @ApiResponse(responseCode = "404", description = "No route available")
    @ApiResponse(responseCode = "503", description = "Service timeout")
    public RouteResponse calculateRoute(
        @Parameter(description = "Start coordinate (latitude,longitude)", example = "52.2297,21.0122")
        @RequestParam
        @NotBlank(message = "Start coordinate is required")
        @Pattern(regexp = "^\\s*-?\\d+(\\.\\d+)?\\s*,\\s*-?\\d+(\\.\\d+)?\\s*$",
                 message = "Invalid coordinate format. Expected: 'lat,lon' (e.g., '52.2297,21.0122')")
        String start,

        @Parameter(description = "End coordinate (latitude,longitude)", example = "52.2400,21.0250")
        @RequestParam
        @NotBlank(message = "End coordinate is required")
        @Pattern(regexp = "^\\s*-?\\d+(\\.\\d+)?\\s*,\\s*-?\\d+(\\.\\d+)?\\s*$",
                 message = "Invalid coordinate format. Expected: 'lat,lon' (e.g., '52.2400,21.0250')")
        String end
    ) {
        log.info("GET /api/evac/route?start={}&end={}", start, end);
        RouteRequest request = new RouteRequest(start, end);
        return routeApplicationService.calculateRoute(request);
    }
}
