package com.project.skillswap.logic.entity.auth;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.Customizer; // NUEVO
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

// NUEVO (imports CORS)
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List; // NUEVO

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfiguration {

    //#region Dependencies
    private final AuthenticationProvider authenticationProvider;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    //#endregion

    //#region Constructor
    /**
     * Creates a new SecurityConfiguration instance.
     *
     * @param jwtAuthenticationFilter the JWT authentication filter
     * @param authenticationProvider the authentication provider
     */
    public SecurityConfiguration(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            AuthenticationProvider authenticationProvider
    ) {
        this.authenticationProvider = authenticationProvider;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }
    //#endregion

    //#region Security Configuration
    /**
     * Configures the security filter chain.
     *
     * @param http the HttpSecurity object to configure
     * @return the configured SecurityFilterChain
     * @throws Exception if configuration fails
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults()) // NUEVO
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/register/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/**").permitAll()
                        .requestMatchers("/dashboard/**").authenticated()

                        // --- NUEVO: aperturas necesarias para onboarding y prefijo /api ---
                        .requestMatchers("/api/register/**").permitAll()          // NUEVO
                        .requestMatchers("/onboarding/**", "/api/onboarding/**").permitAll() // NUEVO
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()   // NUEVO (CORS preflight)
                        // -------------------------------------------------------------------

                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
    //#endregion

    // =========================
    // #region NUEVO: CORS (DEV)
    // =========================
    /**
     * CORS básico para desarrollo con frontend en http://localhost:4200
     * Ajusta origins y puertos si corresponde.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() { // NUEVO
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(List.of("http://localhost:4200"));      // NUEVO
        cors.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS")); // NUEVO
        cors.setAllowedHeaders(List.of("Authorization","Content-Type","X-Requested-With","Accept","Origin")); // NUEVO
        cors.setExposedHeaders(List.of("Authorization","Content-Disposition")); // NUEVO
        cors.setAllowCredentials(true); // NUEVO

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource(); // NUEVO
        source.registerCorsConfiguration("/**", cors); // NUEVO
        return source; // NUEVO
    }
    // #endregion NUEVO
}
