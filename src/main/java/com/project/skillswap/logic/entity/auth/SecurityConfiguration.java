package com.project.skillswap.logic.entity.auth;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

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

                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/ws-chat/**").permitAll()

                        .requestMatchers("/register/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/**").permitAll()
                        .requestMatchers("/skills/**").permitAll()
                        .requestMatchers("/knowledge-areas/**").permitAll()
                        .requestMatchers("/dashboard/**").authenticated()
                        .requestMatchers("/user-skills/**").permitAll()
                        .requestMatchers("/verification/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/videocall/transcription/*/download").permitAll()
                        .requestMatchers(HttpMethod.GET, "/videocall/transcription/*/download-txt").permitAll()
                        .requestMatchers(HttpMethod.GET, "/videocall/transcription/*/download-pdf").permitAll()
                        .requestMatchers("/videocall/**").authenticated()
                        .requestMatchers("/ws-documents/**").permitAll()
                        .requestMatchers("/api/collaborative-documents/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/communities/create").authenticated()
                        .requestMatchers(HttpMethod.GET, "/communities/accept-invitation").authenticated()
                        .requestMatchers("/communities/my-communities").authenticated()
                        .requestMatchers("/communities/**").permitAll()
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
}
