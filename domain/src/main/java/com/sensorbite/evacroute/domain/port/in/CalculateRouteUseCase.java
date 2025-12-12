package com.sensorbite.evacroute.domain.port.in;

import com.sensorbite.evacroute.domain.model.Coordinate;
import com.sensorbite.evacroute.domain.model.Route;

public interface CalculateRouteUseCase {
    Route calculateRoute(Coordinate start, Coordinate end);
}
