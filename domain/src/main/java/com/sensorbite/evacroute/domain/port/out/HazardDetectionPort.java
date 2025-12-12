package com.sensorbite.evacroute.domain.port.out;

import com.sensorbite.evacroute.domain.model.FloodZone;
import com.sensorbite.evacroute.domain.model.RoadSegment;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface HazardDetectionPort {
    Set<String> detectHazardousSegments(Collection<RoadSegment> segments, List<FloodZone> zones);
}
