package com.flightapp.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@Component
public class ReactiveJwtAuthenticationConverter implements Converter<Jwt, Mono<? extends AbstractAuthenticationToken>> {

    private final org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter delegate;

    public ReactiveJwtAuthenticationConverter() {
        this.delegate = new org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter();
        this.delegate.setJwtGrantedAuthoritiesConverter(this::extractAuthorities);
    }

    private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        Set<GrantedAuthority> auths = new HashSet<>();

        // include default scope/authority converter
        org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter scopesConverter =
                new org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter();
        auths.addAll(Optional.ofNullable(scopesConverter.convert(jwt)).orElse(Collections.emptyList()));

        Object rolesClaim = jwt.getClaims().get("roles");
        if (rolesClaim instanceof String) {
            auths.add(new SimpleGrantedAuthority("ROLE_" + rolesClaim));
        } else if (rolesClaim instanceof Collection<?>) {
            ((Collection<?>) rolesClaim).forEach(r -> auths.add(new SimpleGrantedAuthority("ROLE_" + r.toString())));
        }

        return auths;
    }

    @Override
    public Mono<? extends AbstractAuthenticationToken> convert(Jwt jwt) {
        return new ReactiveJwtAuthenticationConverterAdapter(delegate).convert(jwt);
    }
}
