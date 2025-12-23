package io.hohichh.marketplace.payment.security;


import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@AllArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtValidator jwtValidator;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws  IOException, ServletException {
        String token = extractToken(request);

        if(token != null && jwtValidator.validate(token)) {
            Claims claims = jwtValidator.getClaims(token);
            String userId = claims.getSubject();
            String role = claims.get("role", String.class);

            Authentication auth = getAuthentication(role, userId);
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        filterChain.doFilter(request, response);
    }

    private Authentication getAuthentication(String role, String userId) {
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();

        if(role != null && !role.isEmpty()) {
            String springSecurityRole = role.startsWith("ROLE_") ? role : "ROLE_" + role.toUpperCase();
            authorities = List.of(new SimpleGrantedAuthority(springSecurityRole));
        }

        return new UsernamePasswordAuthenticationToken(
                userId,
                null,
                authorities
        );
    }


    private String extractToken(HttpServletRequest request){
        String header = request.getHeader("Authorization");
        if(header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }

}