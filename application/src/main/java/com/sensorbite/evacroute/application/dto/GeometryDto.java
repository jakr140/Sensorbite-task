package com.sensorbite.evacroute.application.dto;

import java.util.List;

public record GeometryDto(
    String type,
    List<List<Double>> coordinates
) {}
