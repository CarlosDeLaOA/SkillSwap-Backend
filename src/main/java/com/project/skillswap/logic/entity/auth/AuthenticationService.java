package com.project.skillswap.logic.entity.auth;

import com.project.skillswap.logic.entity.Person.Person;
import com.project.skillswap.logic.entity.Person.PersonRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationService {

    //#region Dependencies
    private final PersonRepository personRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    //#endregion

    //#region Constructor
    /**
     * Creates a new AuthenticationService instance.
     *
     * @param personRepository the person repository
     * @param authenticationManager the authentication manager
     * @param passwordEncoder the password encoder
     */
    public AuthenticationService(
            PersonRepository personRepository,
            AuthenticationManager authenticationManager,
            PasswordEncoder passwordEncoder
    ) {
        this.authenticationManager = authenticationManager;
        this.personRepository = personRepository;
        this.passwordEncoder = passwordEncoder;
    }
    //#endregion

    //#region Public Methods
    /**
     * Authenticates a person with email and password.
     *
     * @param input the person object containing email and password
     * @return the authenticated person
     * @throws org.springframework.security.core.AuthenticationException if authentication fails
     */
    public Person authenticate(Person input) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        input.getEmail(),
                        input.getPasswordHash()
                )
        );

        return personRepository.findByEmail(input.getEmail())
                .orElseThrow();
    }
    //#endregion
}