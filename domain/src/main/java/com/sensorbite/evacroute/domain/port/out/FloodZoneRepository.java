package com.sensorbite.evacroute.domain.port.out;

import com.sensorbite.evacroute.domain.model.FloodZone;

import java.time.Instant;
import java.util.List;

public interface FloodZoneRepository {
    List<FloodZone> loadActiveAt(Instant timestamp);
}
