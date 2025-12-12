# Evacuation Route Service

A service for calculating safe evacuation routes that avoid flood zones. Implements a modified Dijkstra algorithm with GeoJSON-based road networks and hazard zones, built on hexagonal architecture principles.

## Key Features

- Modified Dijkstra pathfinding with configurable hazard penalties
- GeoJSON-based road network and flood zone data storage
- Spatial indexing (JTS STRtree) for efficient hazard intersection detection
- Support for one-way streets and bidirectional roads
- Temporal validity for flood zones (ISO 8601 timestamps)
- RESTful API with OpenAPI/Swagger documentation
- Production-ready Docker setup with health checks

## Technology Stack

- **Java 21**: Records, sealed types, pattern matching, text blocks
- **Maven 3.9+**: Multi-module build
- **Spring Boot 3.2.1**: Web framework, dependency injection, actuator
- **GeoTools 31.0**: GeoJSON parsing and JTS geometry operations
- **Lombok 1.18.30**: Boilerplate reduction
- **MapStruct 1.5.5**: DTO mapping
- **Springdoc OpenAPI 2.3.0**: API documentation

## Quick Start

### Prerequisites

- Docker and Docker Compose (for running the service)
- Java 21 and Maven 3.9+ (for local development)

### Using Docker Compose

Start the service:

```bash
docker compose up --build
```

The service will be available at:
- **API**: http://localhost:8080/api/evac/route
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **Health Check**: http://localhost:8080/actuator/health

Test the API:

```bash
curl "http://localhost:8080/api/evac/route?start=52.2297,21.0122&end=52.2400,21.0250"
```

Stop the service:

```bash
docker compose down --volumes
```

## Local Development

### Build and Test

Build the entire project:

```bash
mvn clean verify
```

Run tests:

```bash
# All tests
mvn clean test

# Specific module
mvn test -pl domain
mvn test -pl application
mvn test -pl infrastructure
```

The project includes comprehensive test coverage:
- **Domain**: 78 tests (graph building, Dijkstra algorithm, geometry validation)
- **Application**: 44 tests (use cases, mappers, DTOs)
- **Infrastructure**: 29 tests (REST controllers, file adapters, error handling)

### Run Locally

Start the application:

```bash
mvn spring-boot:run -pl infrastructure
```

The service uses `DATA_DIR` environment variable for data file paths (defaults to `data/`).

## API Usage

### Calculate Route

**Endpoint**: `GET /api/evac/route`

**Parameters**:
- `start` (required): Start coordinate in `latitude,longitude` format
- `end` (required): End coordinate in `latitude,longitude` format

**Coordinate Format**:
- **Query parameters**: `latitude,longitude` (e.g., `52.2297,21.0122`)
- **GeoJSON response**: `[longitude,latitude]` (GeoJSON RFC 7946 standard)

This follows the convention where human-readable coordinates use lat,lon but GeoJSON uses lon,lat to align with x,y coordinate ordering.

### Example: Successful Route

Request:

```bash
curl -X GET "http://localhost:8080/api/evac/route?start=52.2297,21.0122&end=52.2400,21.0250" \
  -H "Accept: application/json"
```

Response (200 OK):

```json
{
  "type": "Feature",
  "geometry": {
    "type": "LineString",
    "coordinates": [
      [21.0122, 52.2297],
      [21.0145, 52.2310],
      [21.0200, 52.2320],
      [21.0250, 52.2400]
    ]
  },
  "properties": {
    "distanceMeters": 1523.5,
    "computationTimeMs": 45,
    "hazardousSegmentsAvoided": 2,
    "safetyScore": 0.95,
    "timestamp": "2025-12-12T10:30:45.123Z",
    "allPathsHazardous": false
  }
}
```

**Properties Explained**:
- `distanceMeters`: Total route distance using Haversine formula
- `computationTimeMs`: Time spent calculating the route
- `hazardousSegmentsAvoided`: Number of road segments intersecting flood zones
- `safetyScore`: Ratio of safe to total segments (1.0 = completely safe)
- `allPathsHazardous`: If true, the returned route passes through hazards (no safe alternative exists)

### Example: No Route Available

Request:

```bash
curl -X GET "http://localhost:8080/api/evac/route?start=52.2297,21.0122&end=52.9999,21.9999" \
  -H "Accept: application/json"
```

Response (404 Not Found):

```json
{
  "timestamp": "2025-12-12T10:35:22.456Z",
  "status": 404,
  "error": "Not Found",
  "message": "No route found between start and end coordinates",
  "path": "/api/evac/route"
}
```

### Example: Invalid Coordinates

Request:

```bash
curl -X GET "http://localhost:8080/api/evac/route?start=invalid&end=52.2400,21.0250" \
  -H "Accept: application/json"
```

Response (400 Bad Request):

```json
{
  "timestamp": "2025-12-12T10:40:15.789Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid coordinate format: invalid",
  "path": "/api/evac/route"
}
```

### Example: Computation Timeout

Response (503 Service Unavailable):

```json
{
  "timestamp": "2025-12-12T10:45:00.123Z",
  "status": 503,
  "error": "Service Unavailable",
  "message": "Route computation exceeded maximum time of 30 seconds",
  "path": "/api/evac/route"
}
```

### Interactive API Documentation

Access Swagger UI at http://localhost:8080/swagger-ui.html to:
- Test endpoints interactively
- View request/response schemas
- Explore API definitions

OpenAPI specification available at http://localhost:8080/api-docs

## Architecture

This project follows hexagonal architecture (ports and adapters pattern) with clear separation of concerns.

### Module Structure

```
infrastructure --> application --> domain
```

- **domain**: Pure Java business logic, no external dependencies
  - Core entities (Node, Edge, HazardZone, Route)
  - Domain services (DijkstraPathfinder, RoadNetworkGraphBuilder)
  - Port interfaces (inbound: CalculateRouteUseCase, outbound: RoadNetworkRepository, HazardZoneRepository)

- **application**: Use cases and DTOs
  - Application services implementing domain ports
  - DTOs for external communication (RouteRequest, RouteResponse)
  - MapStruct mappers for domain <-> DTO conversion

- **infrastructure**: Adapters and framework integration
  - REST controllers (inbound adapter)
  - File-based repositories (outbound adapter)
  - Spring Boot configuration and wiring

### Dependency Flow

```
REST Controller
    |
    v
CalculateRouteService (application)
    |
    v
DijkstraPathfinder (domain)
    |
    v
RoadNetworkRepository (port, impl in infrastructure)
```

### Key Design Patterns

- **Hexagonal Architecture**: Domain logic independent of frameworks
- **Dependency Inversion**: Domain defines ports, infrastructure implements adapters
- **Repository Pattern**: Abstract data access through interfaces
- **DTO Pattern**: Separate domain models from API contracts
- **Strategy Pattern**: Configurable hazard penalty calculation

## Configuration

Configuration is managed through `/mnt/d/Users/admin/Desktop/Sensorbitev10/infrastructure/src/main/resources/application.yml`.

### Key Properties

```yaml
server:
  port: 8080

routing:
  hazard-penalty-factor: 10000              # Multiplier for hazardous edges
  max-computation-time-seconds: 30          # Timeout for route calculation
  max-distance-kilometers: 200              # Max straight-line distance
  data:
    road-network-path: data/sample-road-network.geojson
    flood-zones-path: data/sample-flood-zones.geojson

management:
  endpoints:
    web:
      exposure:
        include: health,info              # Actuator endpoints

logging:
  level:
    root: INFO
    com.sensorbite.evacroute: DEBUG       # Application logging
```

### Environment Variables

- `DATA_DIR`: Override data directory path (default: `data/`)
- `SPRING_PROFILES_ACTIVE`: Activate Spring profile (e.g., `docker`)
- `LOGGING_LEVEL_ROOT`: Override root log level

### Docker vs Local Profiles

The service automatically detects the Docker environment when `SPRING_PROFILES_ACTIVE=docker` is set (configured in docker-compose.yml).

## Sample Data

The service includes sample data files in `/mnt/d/Users/admin/Desktop/Sensorbitev10/data/`:

### Road Network (`sample-road-network.geojson`)

A small road network with 6 bidirectional road segments in Warsaw, Poland.

**Format**:
- GeoJSON FeatureCollection
- Features must be LineString or MultiLineString geometries
- Optional `oneway` property: `"yes"`, `"true"`, or `"1"` for one-way streets
- Default is bidirectional if `oneway` is not specified

**Example Feature**:

```json
{
  "type": "Feature",
  "id": "road_1",
  "properties": {
    "oneway": "no"
  },
  "geometry": {
    "type": "LineString",
    "coordinates": [
      [21.0122, 52.2297],
      [21.0145, 52.2310]
    ]
  }
}
```

### Flood Zones (`sample-flood-zones.geojson`)

A single rectangular flood zone polygon covering part of the road network.

**Format**:
- GeoJSON FeatureCollection
- Features must be Polygon geometries
- Optional `validFrom` and `validUntil` properties (ISO 8601 format)
- Zones without timestamps are considered always valid

**Example Feature**:

```json
{
  "type": "Feature",
  "id": "flood_zone_1",
  "properties": {
    "validFrom": "2025-12-01T00:00:00Z",
    "validUntil": "2025-12-31T23:59:59Z"
  },
  "geometry": {
    "type": "Polygon",
    "coordinates": [
      [
        [21.0170, 52.2325],
        [21.0170, 52.2365],
        [21.0230, 52.2365],
        [21.0230, 52.2325],
        [21.0170, 52.2325]
      ]
    ]
  }
}
```

### Using Custom Data

Replace the sample GeoJSON files in the `data/` directory with your own:

1. Ensure valid GeoJSON FeatureCollection format
2. Road network: LineString features with optional `oneway` property
3. Flood zones: Polygon features with optional temporal validity
4. Update `application.yml` if using different filenames
5. Restart the service

The service validates geometries and logs warnings for invalid features.

## Testing

### Run All Tests

```bash
mvn clean test
```

### Run Module-Specific Tests

```bash
# Domain layer tests (78 tests)
mvn test -pl domain

# Application layer tests (44 tests)
mvn test -pl application

# Infrastructure layer tests (29 tests)
mvn test -pl infrastructure
```

### Test Coverage

The project includes 151 tests covering:

- **Domain Layer**:
  - Graph construction from GeoJSON features
  - Dijkstra pathfinding with and without hazards
  - Coordinate validation and geometry intersection
  - Haversine distance calculation
  - Edge weight penalty calculation

- **Application Layer**:
  - Route calculation use cases
  - DTO mapping (MapStruct)
  - Coordinate parsing and validation
  - Error handling and exception mapping

- **Infrastructure Layer**:
  - REST API endpoints (success and error cases)
  - GeoJSON file parsing
  - Spring Boot integration
  - Global exception handling

### Integration Tests

Integration tests run automatically during `mvn verify`:

```bash
mvn clean verify
```

These tests validate the entire stack from REST controller to domain logic.

## Deployment

### Docker Commands

Build the image:

```bash
docker compose build
```

Start in detached mode:

```bash
docker compose up -d
```

View logs:

```bash
docker compose logs -f evac-route-service
```

Check service health:

```bash
docker compose ps
```

Stop and remove volumes:

```bash
docker compose down --volumes
```

### Docker Infrastructure Details

The production-grade Docker setup includes:

- **Multi-stage build**: Separate build and runtime stages for minimal image size
- **Alpine-based images**: Small footprint (maven:3.9-eclipse-temurin-21-alpine, eclipse-temurin:21-jre-alpine)
- **Non-root user**: Runs as appuser (UID 1001) for security
- **Layer caching**: Optimized dependency download layer for faster rebuilds
- **Health checks**: Automated monitoring via /actuator/health (10s interval, 40s start period)
- **Read-only volumes**: Data directory mounted read-only for safety
- **Restart policy**: unless-stopped for resilience

### Health Monitoring

The service exposes Spring Boot Actuator health endpoint:

```bash
curl http://localhost:8080/actuator/health
```

Expected response:

```json
{
  "status": "UP"
}
```

Docker Compose health checks run every 10 seconds with a 40-second startup grace period.

### Resource Requirements

**Minimum**:
- CPU: 1 core
- RAM: 512MB
- Disk: 100MB

**Recommended** (for production workloads):
- CPU: 2 cores
- RAM: 1GB
- Disk: 500MB

**Memory Usage Estimates**:
- Base heap: 256MB
- 10K road segments: ~50MB
- 1K flood zones: ~20MB

### Troubleshooting

**Service won't start**:
- Check Docker logs: `docker compose logs evac-route-service`
- Verify data files exist in `/mnt/d/Users/admin/Desktop/Sensorbitev10/data/`
- Ensure port 8080 is not in use

**Health check failing**:
- Wait for startup period (40 seconds)
- Check application logs for exceptions
- Verify GeoJSON files are valid

**No route found**:
- Ensure coordinates fall within road network bounds
- Check if flood zones block all paths (look for `allPathsHazardous: true`)
- Verify straight-line distance is under 200km

**Slow performance**:
- Large datasets may require increased heap size
- Adjust `routing.max-computation-time-seconds` if needed
- Consider data pruning or spatial partitioning

## Project Structure

```
/mnt/d/Users/admin/Desktop/Sensorbitev10/
├── domain/                          # Pure Java domain logic
│   ├── src/main/java/
│   │   └── com/sensorbite/evacroute/domain/
│   │       ├── model/              # Entities and value objects
│   │       │   ├── Coordinate.java
│   │       │   ├── Edge.java
│   │       │   ├── HazardZone.java
│   │       │   ├── Node.java
│   │       │   ├── RoadNetwork.java
│   │       │   └── Route.java
│   │       ├── service/            # Domain services
│   │       │   ├── DijkstraPathfinder.java
│   │       │   ├── RoadNetworkGraphBuilder.java
│   │       │   └── HaversineDistanceCalculator.java
│   │       └── port/               # Interfaces
│   │           ├── in/
│   │           │   └── CalculateRouteUseCase.java
│   │           └── out/
│   │               ├── RoadNetworkRepository.java
│   │               └── HazardZoneRepository.java
│   └── pom.xml
├── application/                    # Use cases and DTOs
│   ├── src/main/java/
│   │   └── com/sensorbite/evacroute/application/
│   │       ├── dto/               # Request/response objects
│   │       │   ├── RouteRequest.java
│   │       │   ├── RouteResponse.java
│   │       │   └── GeoJsonFeature.java
│   │       ├── mapper/            # MapStruct mappers
│   │       │   ├── CoordinateMapper.java
│   │       │   └── RouteMapper.java
│   │       └── service/           # Application services
│   │           └── CalculateRouteService.java
│   └── pom.xml
├── infrastructure/                # Adapters and frameworks
│   ├── src/main/java/
│   │   └── com/sensorbite/evacroute/infrastructure/
│   │       ├── adapter/
│   │       │   ├── in/rest/      # REST controllers
│   │       │   │   ├── RouteController.java
│   │       │   │   └── GlobalExceptionHandler.java
│   │       │   └── out/file/     # File-based repositories
│   │       │       ├── GeoJsonRoadNetworkRepository.java
│   │       │       └── GeoJsonHazardZoneRepository.java
│   │       ├── config/           # Spring configuration
│   │       │   └── ApplicationConfig.java
│   │       └── EvacRouteApplication.java
│   ├── src/main/resources/
│   │   └── application.yml
│   └── pom.xml
├── data/                          # Sample data files
│   ├── sample-road-network.geojson
│   └── sample-flood-zones.geojson
├── Dockerfile                     # Multi-stage Docker build
├── docker-compose.yml             # Docker Compose configuration
├── pom.xml                        # Parent POM
└── README.md
```

## Technical Details

### Algorithm

**Pathfinding**: Modified Dijkstra's algorithm with priority queue (PriorityQueue backed by binary heap).

**Hazard Avoidance Strategy**:
1. Build graph from road network (nodes at intersections, edges for road segments)
2. Query flood zones that intersect each edge using spatial index
3. Multiply edge weight by `hazard-penalty-factor` (default 10000) for hazardous segments
4. Run Dijkstra from start to nearest node, then to nearest end node
5. Return path with metadata (distance, safety score, computation time)

**Spatial Indexing**: JTS STRtree (Sort-Tile-Recursive tree, variant of R-tree) provides O(log n) average-case flood zone intersection queries.

**Distance Calculation**: Haversine formula for great-circle distance on a sphere. Accurate within 0.5% for distances under 1000km on WGS84 ellipsoid.

### Coordinate Systems

- **WGS84 (EPSG:4326)**: All coordinates use decimal degrees
- **GeoJSON**: [longitude, latitude] ordering per RFC 7946
- **Query parameters**: latitude,longitude ordering (human-readable convention)

### Limitations

- **Scale**: Designed for city-scale networks (tested up to 100K road segments)
- **Distance**: Maximum 200km straight-line distance between start and end
- **Timeout**: Route calculation limited to 30 seconds (configurable)
- **Coordinate System**: WGS84 only, Haversine distance valid for <1000km
- **Antimeridian**: Not handled (evacuation routes typically don't cross ±180° longitude)
- **Topology**: No elevation data; routes are purely 2D

## Contributing

### Code Style

- Use Java 21 idioms: records, sealed types, pattern matching, text blocks
- Prefer streams/functional style over loops when readable
- Avoid arrays in domain/application layers; use collections and Stream API
- Use Lombok (@Data, @Builder, @Slf4j) to reduce boilerplate
- Use MapStruct for DTO mapping

### Pull Request Process

1. Run tests: `mvn clean verify`
2. Ensure all tests pass (151 tests expected)
3. Verify Docker build: `docker compose up --build`
4. Update documentation if API changes
5. Follow existing package structure and naming conventions

### Test Requirements

- Unit tests for new domain logic
- Integration tests for new endpoints
- Minimum 80% code coverage for new code

## License

Prototype for internal use.

## Support

For detailed planning and design decisions, refer to:
- `/mnt/d/Users/admin/Desktop/Sensorbitev10/docs/plan.md` (if exists)
- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI specification: http://localhost:8080/api-docs

For issues or questions, review the architecture section above and consult the inline code documentation.
