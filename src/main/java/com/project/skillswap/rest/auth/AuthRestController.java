package com.project.skillswap.rest.auth;

import com.project.skillswap.logic.entity.Person.LoginResponse;
import com.project.skillswap.logic.entity.Person.Person;
import com.project.skillswap.logic.entity.Person.PersonRepository;
import com.project.skillswap.logic.entity.auth.AuthenticationService;
import com.project.skillswap.logic.entity.auth.JwtService;
import com.project.skillswap.logic.entity.auth.GoogleOAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RequestMapping("/auth")
@RestController
public class AuthRestController {
    //#region Dependencies
    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final AuthenticationService authenticationService;
    private final JwtService jwtService;
    private final GoogleOAuthService googleOAuthService;
    //#endregion

    //#region Constructor
    public AuthRestController(
            JwtService jwtService,
            AuthenticationService authenticationService,
            GoogleOAuthService googleOAuthService) {
        this.jwtService = jwtService;
        this.authenticationService = authenticationService;
        this.googleOAuthService = googleOAuthService;
    }
    //#endregion

    //#region Traditional Authentication
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
    //#endregion

    //#region Google OAuth Endpoints
    /**
     * Endpoint para autenticación con Google OAuth2.
     * @param requestBody Mapa con code y redirectUri
     * @return ResponseEntity con JWT y datos del usuario
     */
    @PostMapping("/google")
    public ResponseEntity<Map<String, Object>> authenticateWithGoogle(@RequestBody Map<String, String> requestBody) {
        try {
            String code = requestBody.get("code");
            String redirectUri = requestBody.get("redirectUri");

            if (code == null || code.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(createErrorResponse("Código de autorización requerido"));
            }

            if (redirectUri == null || redirectUri.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(createErrorResponse("URI de redirección requerida"));
            }

            Map<String, Object> tokenResponse = googleOAuthService.exchangeCodeForToken(code, redirectUri);
            String accessToken = (String) tokenResponse.get("accessToken");

            Map<String, Object> userInfo = googleOAuthService.getUserInfo(accessToken);

            Boolean verifiedEmail = (Boolean) userInfo.get("verifiedEmail");
            if (userInfo.get("email") == null || !Boolean.TRUE.equals(verifiedEmail)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(createErrorResponse("Email no verificado"));
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
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error al autenticar con Google: " + e.getMessage()));
        }
    }

    /**
     * Obtiene la URL de autorización de Google
     */
    @GetMapping("/google/url")
    public ResponseEntity<Map<String, String>> getGoogleAuthUrl() {
        String authUrl = "https://accounts.google.com/o/oauth2/v2/auth?" +
                "client_id=355722441377-enqh4cmujnjnmt0thtl0nbsfinlhsq78.apps.googleusercontent.com&" +
                "redirect_uri=http://localhost:4200/auth/callback&" +
                "response_type=code&" +
                "scope=openid%20profile%20email&" +
                "access_type=offline&" +
                "prompt=consent";

        Map<String, String> response = new HashMap<>();
        response.put("url", authUrl);
        return ResponseEntity.ok(response);
    }

    /**
     * Verifica el estado de la sesión de Google
     */
    @GetMapping("/google/status")
    public ResponseEntity<Map<String, Object>> checkGoogleAuthStatus(@RequestHeader("Authorization") String token) {
        try {
            String jwt = token.replace("Bearer ", "");

            Map<String, Object> response = new HashMap<>();
            response.put("authenticated", true);
            response.put("token", jwt);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(createErrorResponse("Sesión inválida"));
        }
    }

    /**
     * Cierra sesión de Google
     */
    @PostMapping("/google/logout")
    public ResponseEntity<Map<String, Object>> googleLogout(@RequestHeader("Authorization") String token) {
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Sesión cerrada exitosamente");
            response.put("success", true);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error al cerrar sesión"));
        }
    }
    //#endregion

    //#region Helper Methods
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

    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("error", message);
        error.put("success", false);
        return error;
    }
    //#endregion
}