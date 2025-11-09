package com.project.skillswap.rest.auth;

import com.project.skillswap.logic.entity.Person.LoginResponse;
import com.project.skillswap.logic.entity.Person.Person;
import com.project.skillswap.logic.entity.Person.PersonRepository;
import com.project.skillswap.logic.entity.auth.AuthenticationService;
import com.project.skillswap.logic.entity.auth.GoogleOAuthService;
import com.project.skillswap.logic.entity.auth.JwtService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/auth") // con context-path /api -> /api/auth/**
@CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
public class AuthRestController {

    private final PersonRepository personRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationService authenticationService;
    private final JwtService jwtService;
    private final GoogleOAuthService googleOAuthService;

    public AuthRestController(
            PersonRepository personRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationService authenticationService,
            JwtService jwtService,
            GoogleOAuthService googleOAuthService
    ) {
        this.personRepository = personRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationService = authenticationService;
        this.jwtService = jwtService;
        this.googleOAuthService = googleOAuthService;
    }

    // =========================
    // Registro tradicional
    // =========================
    @PostMapping(path = "/register", consumes = "application/json", produces = "application/json")
    public ResponseEntity<?> register(@RequestBody Map<String, Object> payload) {
        final String name      = str(payload.get("name"));
        final String lastname  = str(payload.get("lastname"));
        final String email     = str(payload.get("email")).toLowerCase();
        final String password  = str(payload.get("password"));

        // Validaciones básicas
        if (isBlank(name) || isBlank(lastname) || isBlank(email) || isBlank(password)) {
            return badRequest("Faltan campos obligatorios: name, lastname, email, password");
        }

        // Duplicado
        if (personRepository.findByEmail(email).isPresent()) {
            return conflict("El email ya está registrado");
        }

        // Persistencia
        Person p = new Person();
        p.setFullName(name + " " + lastname);
        p.setEmail(email);
        p.setPasswordHash(passwordEncoder.encode(password));

        Person saved = personRepository.save(p);

        // Respuesta: token + usuario (estilo login)
        String jwtToken = jwtService.generateToken(saved);

        LoginResponse resp = new LoginResponse();
        resp.setToken(jwtToken);
        resp.setExpiresIn(jwtService.getExpirationTime());
        resp.setAuthPerson(saved);

        // 201 Created + Location
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create("/api/auth/status"));
        return new ResponseEntity<>(resp, headers, HttpStatus.CREATED);
    }
    @GetMapping(path = "/register/check-email", produces = "application/json")
    public ResponseEntity<Map<String, Boolean>> checkEmail(@RequestParam("email") String email) {
        boolean exists =
                personRepository.findByEmailIgnoreCase(email).isPresent(); // o existsByEmailIgnoreCase si lo tienes
        Map<String, Boolean> body = new HashMap<>();
        body.put("exists", exists);
        return ResponseEntity.ok(body);
    }

    // =========================
    // Login tradicional
    // =========================
    @PostMapping(path = "/login", consumes = "application/json", produces = "application/json")
    public ResponseEntity<LoginResponse> authenticate(@RequestBody Map<String, Object> payload) {
        final String email = str(payload.get("email"));
        final String rawPassword =
                payload.get("password") != null ? str(payload.get("password"))
                        : payload.get("passwordHash") != null ? str(payload.get("passwordHash"))
                        : null;

        Person loginPerson = new Person();
        loginPerson.setEmail(email);
        loginPerson.setPasswordHash(rawPassword);

        Person authenticatedUser = authenticationService.authenticate(loginPerson);
        String jwtToken = jwtService.generateToken(authenticatedUser);

        LoginResponse loginResponse = new LoginResponse();
        loginResponse.setToken(jwtToken);
        loginResponse.setExpiresIn(jwtService.getExpirationTime());

        Optional<Person> found = personRepository.findByEmail(email);
        found.ifPresent(loginResponse::setAuthPerson);

        return ResponseEntity.ok(loginResponse);
    }

    // =========================
    // Estado rápido
    // =========================
    @GetMapping(path = "/status", produces = "application/json")
    public ResponseEntity<Map<String, Object>> status() {
        Map<String, Object> ok = new HashMap<>();
        ok.put("status", "OK");
        return ResponseEntity.ok(ok);
    }

    // =========================
    // Google OAuth
    // =========================
    @PostMapping(path = "/google", consumes = "application/json", produces = "application/json")
    public ResponseEntity<Map<String, Object>> authenticateWithGoogle(@RequestBody Map<String, String> requestBody) {
        try {
            String code = str(requestBody.get("code"));
            String redirectUri = str(requestBody.get("redirectUri"));

            if (isBlank(code)) {
                return badRequest("Código de autorización requerido");
            }
            if (isBlank(redirectUri)) {
                return badRequest("URI de redirección requerida");
            }

            Map<String, Object> tokenResponse = googleOAuthService.exchangeCodeForToken(code, redirectUri);
            String accessToken = (String) tokenResponse.get("accessToken");

            Map<String, Object> userInfo = googleOAuthService.getUserInfo(accessToken);
            Boolean verifiedEmail = (Boolean) userInfo.get("verifiedEmail");
            if (userInfo.get("email") == null || !Boolean.TRUE.equals(verifiedEmail)) {
                return badRequest("Email no verificado");
            }

            Person person = googleOAuthService.processGoogleUser(userInfo);
            String jwtToken = jwtService.generateToken(person);

            Map<String, Object> response = new HashMap<>();
            response.put("token", jwtToken);
            response.put("tokenType", "Bearer");
            response.put("expiresIn", jwtService.getExpirationTime());
            response.put("profile", createUserResponse(person));
            response.put("requiresOnboarding", googleOAuthService.requiresOnboarding(person));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return serverError("Error al autenticar con Google: " + e.getMessage());
        }
    }

    @GetMapping(path = "/google/url", produces = "application/json")
    public ResponseEntity<Map<String, String>> getGoogleAuthUrl() {
        String authUrl = "https://accounts.google.com/o/oauth2/v2/auth?"
                + "client_id=355722441377-enqh4cmujnjnmt0thtl0nbsfinlhsq78.apps.googleusercontent.com&"
                + "redirect_uri=http://localhost:4200/auth/callback&"
                + "response_type=code&"
                + "scope=openid%20profile%20email&"
                + "access_type=offline&"
                + "prompt=consent";

        Map<String, String> response = new HashMap<>();
        response.put("url", authUrl);
        return ResponseEntity.ok(response);
    }

    @GetMapping(path = "/google/status", produces = "application/json")
    public ResponseEntity<Map<String, Object>> checkGoogleAuthStatus(@RequestHeader(value = "Authorization", required = false) String token) {
        Map<String, Object> response = new HashMap<>();
        response.put("authenticated", token != null && token.startsWith("Bearer "));
        response.put("token", token != null ? token.replace("Bearer ", "") : null);
        return ResponseEntity.ok(response);
    }

    @PostMapping(path = "/google/logout", produces = "application/json")
    public ResponseEntity<Map<String, Object>> googleLogout(@RequestHeader(value = "Authorization", required = false) String token) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Sesión cerrada exitosamente");
        response.put("success", true);
        return ResponseEntity.ok(response);
    }

    // =========================
    // Helpers
    // =========================
    private static String str(Object o) {
        return o == null ? null : o.toString().trim();
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private Map<String, Object> createUserResponse(Person person) {
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("id", person.getId());
        userMap.put("email", person.getEmail());
        userMap.put("fullName", person.getFullName());
        userMap.put("profileImage", person.getProfilePhotoUrl());
        userMap.put("preferredLanguage", person.getPreferredLanguage());
        userMap.put("oauthProvider", person.getGoogleOauthId() != null ? "google" : null);
        return userMap;
    }

    private ResponseEntity<Map<String, Object>> badRequest(String msg) {
        Map<String, Object> error = new HashMap<>();
        error.put("error", msg);
        error.put("success", false);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    private ResponseEntity<Map<String, Object>> conflict(String msg) {
        Map<String, Object> error = new HashMap<>();
        error.put("error", msg);
        error.put("success", false);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    private ResponseEntity<Map<String, Object>> serverError(String msg) {
        Map<String, Object> error = new HashMap<>();
        error.put("error", msg);
        error.put("success", false);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
