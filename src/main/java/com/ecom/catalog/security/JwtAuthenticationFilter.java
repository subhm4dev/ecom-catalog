package com.ecom.catalog.security;

import com.ecom.jwt.blocking.BlockingJwtValidationService;
import com.nimbusds.jwt.JWTClaimsSet;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT Authentication Filter
 * 
 * <p>Spring Security filter that extracts JWT from Authorization header,
 * validates it, and creates JwtAuthenticationToken for Spring Security context.
 * 
 * <p>This filter runs before Spring Security's authentication chain.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final BlockingJwtValidationService jwtValidationService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        // Extract JWT from Authorization header
        String authorizationHeader = request.getHeader("Authorization");
        
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            // No JWT token - let Spring Security handle it (will reject if endpoint requires auth)
            log.debug("No Authorization header found, continuing filter chain");
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // Extract token
            String token = authorizationHeader.substring(7); // Remove "Bearer " prefix

            // Validate token and extract claims
            JWTClaimsSet claims = jwtValidationService.validateToken(token);
            
            // Extract user context from validated JWT claims (source of truth)
            String userId = jwtValidationService.extractUserId(claims);
            String tenantId = jwtValidationService.extractTenantId(claims);
            List<String> roles = jwtValidationService.extractRoles(claims);

            log.debug("JWT validated successfully: userId={}, tenantId={}, roles={}", userId, tenantId, roles);

            // Create authentication token for Spring Security
            JwtAuthenticationToken authentication = new JwtAuthenticationToken(
                userId, tenantId, roles, token
            );

            // Set authentication in SecurityContext
            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (IllegalArgumentException e) {
            // JWT validation failed - this is an authentication error
            log.warn("JWT validation failed: {}", e.getMessage());
            SecurityContextHolder.clearContext();
            // Continue filter chain - Spring Security will handle unauthorized access
            filterChain.doFilter(request, response);
            return;
        } catch (RuntimeException e) {
            // JWT validation failed (e.g., invalid token, expired, etc.)
            log.warn("JWT validation failed: {}", e.getMessage(), e);
            SecurityContextHolder.clearContext();
            // Continue filter chain - Spring Security will handle unauthorized access
            filterChain.doFilter(request, response);
            return;
        } catch (Exception e) {
            // Catch-all for any other checked exceptions during JWT validation
            log.error("Unexpected error during JWT authentication", e);
            SecurityContextHolder.clearContext();
            filterChain.doFilter(request, response);
            return;
        }

        // If we get here, authentication was successful
        // Continue filter chain - any exceptions from downstream (like BusinessException)
        // will be handled by GlobalExceptionHandler, not this filter
        try {
            filterChain.doFilter(request, response);
        } finally {
            // Optional: Clear SecurityContext after request completes
            // (Spring Security usually does this automatically, but being explicit)
        }
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        // Skip JWT validation for public endpoints
        // Only 3 catalog endpoints are public (GET only):
        // 1. GET /api/v1/category - Get all categories
        // 2. GET /api/v1/product/search - Search/get all products
        // 3. GET /api/v1/product/{id} - Get product by ID
        String method = request.getMethod();
        String path = request.getRequestURI();
        
        // Skip for actuator, swagger, and API docs
        if (path.startsWith("/actuator") ||
            path.startsWith("/swagger-ui") ||
            path.startsWith("/v3/api-docs")) {
            return true;
        }
        
        // Only allow GET requests for public catalog endpoints
        if (!"GET".equals(method)) {
            return false;
        }
        
        // Exact match for GET /api/v1/category (no query params in path, but that's OK)
        if (path.equals("/api/v1/category")) {
            return true;
        }
        
        // Exact match for GET /api/v1/product/search
        if (path.equals("/api/v1/product/search")) {
            return true;
        }
        
        // Match GET /api/v1/product/{id} where {id} is a UUID (no additional path segments)
        if (path.startsWith("/api/v1/product/") && path.matches("/api/v1/product/[^/]+$")) {
            return true;
        }
        
        return false;
    }
}

