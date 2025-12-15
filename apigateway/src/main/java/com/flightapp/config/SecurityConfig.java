package com.flightapp.config;

import com.flightapp.security.ReactiveJwtAuthenticationConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
        http.csrf(ServerHttpSecurity.CsrfSpec::disable);

        http.authorizeExchange(exchanges -> exchanges
                .pathMatchers("/auth/**", "/public/**", "/actuator/**").permitAll()
                .anyExchange().authenticated()
        );

        http.oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthConverter))
        );

        return http.build();
    }
}
