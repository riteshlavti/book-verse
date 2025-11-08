package com.bookverse.userservice.config;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


public class GatewayHeaderAuthenticationFilter extends OncePerRequestFilter {

    private static final String ROLES_HEADER = "X-User-Roles";
    private static final String USER_ID_HEADER = "X-User-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String rolesString = request.getHeader(ROLES_HEADER);
        String userId = request.getHeader(USER_ID_HEADER);

        if (rolesString == null || rolesString.trim().isEmpty() || userId == null || userId.trim().isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            List<SimpleGrantedAuthority> authorities = Arrays.stream(rolesString.split(","))
                    .map(String::trim)
                    .filter(role -> !role.isEmpty())
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    userId, null, authorities
            );

            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            logger.debug("Successfully set Authentication in Security Context for user: " + userId + " with roles: " + rolesString);

        } catch (Exception e) {
            logger.error("Error processing trusted security headers: " + e.getMessage(), e);
        }

        filterChain.doFilter(request, response);
    }
}
