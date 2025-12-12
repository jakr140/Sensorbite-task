package com.sensorbite.evacroute.infrastructure.config;

import com.sensorbite.evacroute.application.mapper.RouteMapper;
import com.sensorbite.evacroute.application.service.RouteApplicationService;
import com.sensorbite.evacroute.domain.port.out.FloodZoneRepository;
import com.sensorbite.evacroute.domain.port.out.HazardDetectionPort;
import com.sensorbite.evacroute.domain.port.out.RoadNetworkRepository;
import com.sensorbite.evacroute.domain.service.GraphBuilder;
import com.sensorbite.evacroute.domain.service.RouteCalculationService;
import org.mapstruct.factory.Mappers;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeanConfiguration {

    @Bean
    public GraphBuilder graphBuilder() {
        return new GraphBuilder();
    }

    @Bean
    public RouteCalculationService routeCalculationService() {
        return new RouteCalculationService();
    }

    @Bean
    public RouteMapper routeMapper() {
        return Mappers.getMapper(RouteMapper.class);
    }

    @Bean
    public RouteApplicationService routeApplicationService(
            RoadNetworkRepository roadNetworkRepository,
            FloodZoneRepository floodZoneRepository,
            HazardDetectionPort hazardDetectionPort,
            RouteCalculationService routeCalculationService,
            RouteMapper routeMapper
    ) {
        return new RouteApplicationService(
                roadNetworkRepository,
                floodZoneRepository,
                hazardDetectionPort,
                routeCalculationService,
                routeMapper
        );
    }
}
