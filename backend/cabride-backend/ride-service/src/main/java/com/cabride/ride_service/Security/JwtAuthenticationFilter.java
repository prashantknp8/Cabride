package com.cabride.ride_service.Security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String authHeader= request.getHeader("Authorization");

        if(authHeader== null || !authHeader.startsWith("Bearer ")){
            filterChain.doFilter(request,response);
            return;
        }
        System.out.println(">>> JWT FILTER: " + request.getMethod()
                + " " + request.getRequestURI());

        System.out.println(">>> AUTH HEADER PRESENT: "
                + (authHeader != null && authHeader.startsWith("Bearer ")));

        String token= authHeader.substring(7);

        try{
            System.out.println(">>> TOKEN VALID: "
                    + jwtService.isTokenValid(token));


            if(jwtService.isTokenValid(token)){
                UUID userId=jwtService.extractUserId(token);
                String role=jwtService.extractRole(token);

                UsernamePasswordAuthenticationToken authentication=
                        new UsernamePasswordAuthenticationToken(
                                userId,
                                null,
                                List.of(
                                        new SimpleGrantedAuthority(
                                                "ROLE_" + role
                                        )
                                )
                        );

                SecurityContextHolder
                        .getContext().setAuthentication(authentication);


            }

        }
        catch(Exception ex){
            // Invalid token -> request remains unauthenticated
            ex.printStackTrace();
        }
        filterChain.doFilter(request,response);

    }
}
