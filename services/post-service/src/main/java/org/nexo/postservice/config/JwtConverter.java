package org.nexo.postservice.config;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class JwtConverter implements Converter<Jwt, AbstractAuthenticationToken> {
    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = new HashSet<>();

        Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
        if (resourceAccess != null) {
            for (String client : resourceAccess.keySet()) {
                Map<String, Object> clientAccess = (Map<String, Object>) resourceAccess.get(client);
                if (clientAccess.containsKey("roles")) {
                    Collection<String> roles = (Collection<String>) clientAccess.get("roles");
                    roles.forEach(r -> authorities.add(new SimpleGrantedAuthority("ROLE_" + r)));
                }
            }
        }

        return new JwtAuthenticationToken(jwt, authorities);
    }
}