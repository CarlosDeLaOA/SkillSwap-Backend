package com.project.skillswap.logic.entity.auth;

import com.project.skillswap.logic.entity.Person.PersonRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Configuration
public class ApplicationConfiguration {

    //#region Dependencies
    @Autowired
    private final PersonRepository personRepository;
    //#endregion

    //#region Constructor
    /**
     * Creates a new ApplicationConfiguration instance.
     *
     * @param personRepository the person repository
     */
    public ApplicationConfiguration(PersonRepository personRepository) {
        this.personRepository = personRepository;
    }
    //#endregion

    //#region Beans
    /**
     * Provides the UserDetailsService implementation for authentication.
     *
     * @return UserDetailsService that loads person details by email
     */
    @Bean
    UserDetailsService userDetailsService() {
        return username -> personRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    /**
     * Provides the password encoder bean.
     *
     * @return BCryptPasswordEncoder instance
     */
    @Bean
    BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Provides the authentication manager bean.
     *
     * @param config the authentication configuration
     * @return AuthenticationManager instance
     * @throws Exception if authentication manager cannot be created
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Provides the authentication provider bean.
     *
     * @return AuthenticationProvider configured with UserDetailsService and PasswordEncoder
     */
    @Bean
    AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService());
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }
    //#endregion
}