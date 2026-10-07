package com.myfinance.track.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/** The only place that decides "who is calling". The id always comes from the verified token. */
@Component
public class CurrentUser {

    public Long id() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            return Long.valueOf(jwtAuth.getToken().getSubject());
        }
        throw new IllegalStateException("No authenticated user in security context");
    }
}