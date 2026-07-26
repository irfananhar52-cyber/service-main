package com.irfan.order.security;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@Component
public class JwtFilter extends OncePerRequestFilter {

    @Value("${jwt.secret}")
    private String secret;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        System.out.println("AUTH HEADER: " + authHeader);

        // kalau tidak ada token → lanjut (biar endpoint public tetap jalan)
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try {
            // System.out.println("fhfjhfjhfjfhgjjjjjjjjjjjjjjj");
            // Claims claims = 
            // Jwts.parser()
            //         .verifyWith(Keys.hmacShaKeyFor(secret.getBytes(java.nio.charset.StandardCharsets.UTF_8)))
            //         .build()
            //         .parseSignedClaims(token)
            //         .getPayload();


            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(Keys.hmacShaKeyFor(secret.getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            String username = claims.getSubject();
            String role = (String) claims.get("role");
            
            System.out.println("DEBUG: Claims: " + claims);
            System.out.println("DEBUG: Username: " + username);
            System.out.println("DEBUG: Role from token: " + role);

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                if (role == null) {
                    System.out.println("DEBUG: Role is null, checking authorities or other claims...");
                    // Try to get from "authorities" if "role" is missing
                    Object authoritiesObj = claims.get("authorities");
                    if (authoritiesObj instanceof List<?> authList) {
                        if (!authList.isEmpty()) {
                            Object firstAuth = authList.get(0);
                            if (firstAuth instanceof java.util.Map) {
                                role = (String) ((java.util.Map<?, ?>) firstAuth).get("authority");
                            } else {
                                role = firstAuth.toString();
                            }
                            System.out.println("DEBUG: Found role in authorities: " + role);
                        }
                    }
                }

                if (role != null && !role.startsWith("ROLE_")) {
                    role = "ROLE_" + role;
                }

                if (role == null) {
                    role = "ROLE_USER"; // Default if still null
                    System.out.println("DEBUG: Defaulting to ROLE_USER");
                }

                // Clean the role string in case it has brackets or extra spaces from toString()
                role = role.replace("[", "").replace("]", "").trim();

                String finalRole = role;
                List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(finalRole));
                System.out.println("DEBUG: Setting authentication for " + username + " with authorities " + authorities);

                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        username,
                        null,
                        authorities);

                SecurityContextHolder.getContext().setAuthentication(auth);
            }

        } catch (Exception e) {
            System.out.println("===== JWT ERROR START =====");
            System.out.println("Error class: " + e.getClass().getName());
            System.out.println("Error message: " + e.getMessage());
            System.out.println("Stack trace: ");
            e.printStackTrace(System.out);
            System.out.println("===== JWT ERROR END =====");
            
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Token invalid or expired\", \"message\": \"" + e.getMessage() + "\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getServletPath();
        return path.startsWith("/v3/api-docs") ||
            path.startsWith("/swagger-ui") ||
            path.startsWith("/actuator");
    }
}