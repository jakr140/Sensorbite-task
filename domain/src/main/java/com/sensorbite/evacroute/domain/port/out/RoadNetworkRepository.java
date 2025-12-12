package com.sensorbite.evacroute.domain.port.out;

import com.sensorbite.evacroute.domain.model.RoadNetwork;

public interface RoadNetworkRepository {
    RoadNetwork load();
}
