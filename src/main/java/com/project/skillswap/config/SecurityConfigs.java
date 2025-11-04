package com.project.skillswap.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfigs {

//    @Bean
//    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
//        http
//                .csrf(csrf -> csrf.disable())
//                .authorizeHttpRequests(auth -> auth
//                        // Público para flujo de reset y pruebas de mail
//                        .requestMatchers(
//                                "/auth/password-reset/**",   // /request, /confirm, /validate si los usas
//                                "/test-mail",                // endpoint de prueba
//                                "/test-email"                // si usas este nombre
//                        ).permitAll()
//
//                        // (Opcional) Público para recursos estáticos / Angular
//                        .requestMatchers(HttpMethod.GET,
//                                "/", "/index.html", "/favicon.ico", "/assets/**", "/**/*.js", "/**/*.css"
//                        ).permitAll()
//
//                        // Todo lo demás protegido
//                        .anyRequest().authenticated()
//                )
//                .httpBasic(Customizer.withDefaults());
//
//        return http.build();
//    }
}
