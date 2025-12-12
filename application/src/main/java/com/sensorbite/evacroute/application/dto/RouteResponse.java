package com.sensorbite.evacroute.application.dto;

import java.util.List;

public record RouteResponse(
    String type,
    GeometryDto geometry,
    PropertiesDto properties
) {}
