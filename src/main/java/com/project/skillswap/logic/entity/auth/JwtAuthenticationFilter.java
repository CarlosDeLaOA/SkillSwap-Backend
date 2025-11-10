package com.project.skillswap.logic.entity.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import org.springframework.util.AntPathMatcher;
import java.util.Set;
import java.io.IOException;


@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    //#region Dependencies
    private final HandlerExceptionResolver handlerExceptionResolver;
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    //#endregion


    // =========================
    // #region NUEVA: WHITELIST
    // =========================
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher(); // NUEVO
    private static final Set<String> PUBLIC_PATTERNS = Set.of( // NUEVO
            "/auth/**",
            "/api/auth/**",
            "/register/**",
            "/api/register/**",
            "/onboarding/**",
            "/api/onboarding/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/error"
    );

    /** Saltar filtro para preflight y rutas públicas */ // NUEVO

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        // Preflight CORS
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true;

        String path = request.getRequestURI();
        for (String pattern : PUBLIC_PATTERNS) {
            if (PATH_MATCHER.match(pattern, path)) return true;
        }
        return false;
    }
    // #endregion NUEVO

    //#region Constructor
    /**
     * Creates a new JwtAuthenticationFilter instance.
     *
     * @param jwtService the JWT service
     * @param userDetailsService the user details service
     * @param handlerExceptionResolver the exception resolver
     */
    public JwtAuthenticationFilter(
            JwtService jwtService,
            UserDetailsService userDetailsService,
            HandlerExceptionResolver handlerExceptionResolver
    ) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.handlerExceptionResolver = handlerExceptionResolver;
    }
    //#endregion

    //#region Filter Methods
    /**
     * Processes each request to validate JWT token and set authentication context.
     *
     * @param request the HTTP request
     * @param response the HTTP response
     * @param filterChain the filter chain
     * @throws ServletException if servlet error occurs
     * @throws IOException if I/O error occurs
     */
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            final String jwt = authHeader.substring(7);
            final String userEmail = jwtService.extractUsername(jwt);

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (userEmail != null && authentication == null) {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

                if (jwtService.isTokenValid(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );

                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }

            filterChain.doFilter(request, response);
        } catch (Exception exception) {
            handlerExceptionResolver.resolveException(request, response, null, exception);
        }
    }
    //#endregion
}