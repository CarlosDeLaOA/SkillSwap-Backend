package com.project.skillswap.rest.auth;

import com.project.skillswap.logic.entity.Person.LoginResponse;
import com.project.skillswap.logic.entity.Person.Person;
import com.project.skillswap.logic.entity.Person.PersonRepository;
import com.project.skillswap.logic.entity.auth.AuthenticationService;
import com.project.skillswap.logic.entity.auth.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RequestMapping("/auth")
@RestController
public class AuthRestController {

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final AuthenticationService authenticationService;
    private final JwtService jwtService;

    public AuthRestController(JwtService jwtService, AuthenticationService authenticationService) {
        this.jwtService = jwtService;
        this.authenticationService = authenticationService;
    }

    /**
     * Acepta JSON con:
     * { "email": "...", "password": "..." }
     *  o bien
     * { "email": "...", "passwordHash": "..." }
     * Internamente lo mapea a Person.passwordHash para que el AuthenticationService compare
     * contra el hash de la BD usando passwordEncoder.matches(raw, hash).
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> authenticate(@RequestBody Map<String, Object> payload) {
        String email = payload.get("email") != null ? payload.get("email").toString() : null;

        String rawPassword = null;
        if (payload.get("password") != null) {
            rawPassword = payload.get("password").toString();
        } else if (payload.get("passwordHash") != null) {
            rawPassword = payload.get("passwordHash").toString();
        }

        Person loginPerson = new Person();
        loginPerson.setEmail(email);
        loginPerson.setPasswordHash(rawPassword);

        Person authenticatedUser = authenticationService.authenticate(loginPerson);

        String jwtToken = jwtService.generateToken(authenticatedUser);

        LoginResponse loginResponse = new LoginResponse();
        loginResponse.setToken(jwtToken);
        loginResponse.setExpiresIn(jwtService.getExpirationTime());

        Optional<Person> foundedUser = personRepository.findByEmail(email);
        foundedUser.ifPresent(loginResponse::setAuthPerson);

        return ResponseEntity.ok(loginResponse);
    }
}
