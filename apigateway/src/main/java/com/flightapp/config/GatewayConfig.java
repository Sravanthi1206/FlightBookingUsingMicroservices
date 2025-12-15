package com.flightapp.config;

import com.flightapp.filter.JwtAuthFilter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public GatewayConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("authservice", r -> r.path("/auth/**").uri("lb://authservice"))
                .route("flightservice", r -> r.path("/api/flights/**").uri("lb://flightservice"))
                .route("bookingservice", r -> r.path("/api/bookings/**")
                        .filters(f -> f.filter(jwtAuthFilter.apply(new JwtAuthFilter.Config())))
                        .uri("lb://bookingservice"))
                .route("emailservice", r -> r.path("/api/email/**").uri("lb://emailservice"))
                .build();
    }
}
