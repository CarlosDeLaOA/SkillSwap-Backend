package com.project.skillswap.logic.entity.auth;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import com.project.skillswap.logic.entity.Person.Person;
import com.project.skillswap.logic.entity.Person.PersonRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Servicio de autenticación que valida las credenciales del usuario
 * y verifica que el correo electrónico esté validado.
 */
@Service
public class AuthenticationService {
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationService.class);

    //#region Dependencies
    private final PersonRepository personRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    public AuthenticationService(PersonRepository personRepository,
                                 PasswordEncoder passwordEncoder,
                                 AuthenticationManager authenticationManager) {
        this.personRepository = personRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
    }
    //#endregion

    //#region Public Methods
    /**
     * Autentica un usuario verificando sus credenciales y el estado de verificación de email.
     *
     * @param loginPerson objeto Person con email y password sin encriptar
     * @return Person autenticado si las credenciales son válidas
     * @throws BadCredentialsException si las credenciales son inválidas
     * @throws DisabledException si el email no ha sido verificado
     */
    public Person authenticate(Person loginPerson) {
        String email = loginPerson.getEmail();
        String rawPassword = loginPerson.getPasswordHash();

        Person person = personRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Credenciales inválidas"));

        if (!passwordEncoder.matches(rawPassword, person.getPasswordHash())) {
            throw new BadCredentialsException("Credenciales inválidas");
        }

        if (!person.getEmailVerified()) {
            throw new DisabledException("Por favor verifica tu correo electrónico antes de iniciar sesión");
        }

        if (!person.getActive()) {
            throw new DisabledException("Tu cuenta ha sido desactivada. Contacta al soporte");
        }

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, rawPassword)
        );

        return (Person) authentication.getPrincipal();
    }
    //#endregion
}
