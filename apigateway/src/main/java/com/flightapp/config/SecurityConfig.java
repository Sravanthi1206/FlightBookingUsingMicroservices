package com.flightapp.config;

import com.flightapp.security.ReactiveJwtAuthenticationConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
public class SecurityConfig {

    private final ReactiveJwtAuthenticationConverter jwtAuthConverter;

    public SecurityConfig(ReactiveJwtAuthenticationConverter jwtAuthConverter) {
        this.jwtAuthConverter = jwtAuthConverter;
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {

        http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)

            //  ENABLE CORS (VERY IMPORTANT)
            .cors(cors -> {})

            .authorizeExchange(exchanges -> exchanges
                //  ALLOW PREFLIGHT REQUESTS
                .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                // Public endpoints
                .pathMatchers("/auth/**", "/public/**", "/actuator/**").permitAll()
                // Allow unauthenticated flight search (GET only)
                .pathMatchers(HttpMethod.GET, "/api/flights/**").permitAll()

                // Secure everything else
                .anyExchange().authenticated()
            )

            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthConverter))
            );

        return http.build();
    }
}
