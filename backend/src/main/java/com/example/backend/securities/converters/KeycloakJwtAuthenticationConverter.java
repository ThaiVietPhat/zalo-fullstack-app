package com.example.backend.securities.converters;

import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class KeycloakJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(@NonNull Jwt source) {
        Collection<GrantedAuthority> authorities = Stream.concat(
                new JwtGrantedAuthoritiesConverter().convert(source).stream(),
                extractResourceRoles(source).stream()
        ).collect(Collectors.toSet());
        String email = source.getClaimAsString("email");
        String name = (email != null) ? email : source.getSubject();

        return new JwtAuthenticationToken(source, authorities, name);
    }

    private Collection<? extends GrantedAuthority> extractResourceRoles(Jwt jwt) {
        var resourceAccess = jwt.<Map<String, Object>>getClaim("resource_access");

        if (resourceAccess == null) {
            return Set.of();
        }
        String clientId = jwt.getClaimAsString("azp");

        Map<String, Object> clientAccess = null;
        if (clientId != null && resourceAccess.get(clientId) != null) {
            clientAccess = (Map<String, Object>) resourceAccess.get(clientId);
        } else if (resourceAccess.get("account") != null) {
            clientAccess = (Map<String, Object>) resourceAccess.get("account");
        }

        if (clientAccess == null) {
            return Set.of();
        }

        var roles = (Collection<String>) clientAccess.get("roles");
        if (roles == null) return Set.of();

        return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.replace("-", "_")))
                .collect(Collectors.toSet());
    }
}