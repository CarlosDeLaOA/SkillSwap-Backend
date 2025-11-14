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
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import com.project.skillswap.logic.entity.Learner.Learner;
import com.project.skillswap.logic.entity.Instructor.Instructor;

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
    public ResponseEntity<?> authenticate(@RequestBody Map<String, Object> payload) {
        try {
            System.out.println("[LOGIN] Iniciando proceso de autenticación...");


            String email = payload.get("email") != null ? payload.get("email").toString().trim() : null;

            if (email == null || email.isEmpty()) {
                System.out.println("[LOGIN] Email vacío o nulo");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(createErrorResponse(
                                "INVALID_EMAIL",
                                "El email es requerido",
                                HttpStatus.BAD_REQUEST.value()
                        ));
            }


            String rawPassword = null;
            if (payload.get("password") != null) {
                rawPassword = payload.get("password").toString();
            } else if (payload.get("passwordHash") != null) {
                rawPassword = payload.get("passwordHash").toString();
            }

            if (rawPassword == null || rawPassword.trim().isEmpty()) {
                System.out.println("[LOGIN] Contraseña vacía o nula");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(createErrorResponse(
                                "INVALID_PASSWORD",
                                "La contraseña es requerida",
                                HttpStatus.BAD_REQUEST.value()
                        ));
            }


            if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                System.out.println("[LOGIN] Formato de email inválido: " + email);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(createErrorResponse(
                                "INVALID_EMAIL_FORMAT",
                                "El formato del email no es válido",
                                HttpStatus.BAD_REQUEST.value()
                        ));
            }

            System.out.println("[LOGIN] Validaciones iniciales pasadas para: " + email);

            Optional<Person> userOptional = personRepository.findByEmail(email);

            if (!userOptional.isPresent()) {
                System.out.println("[LOGIN] Usuario no existe: " + email);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse(
                                "USER_NOT_FOUND",
                                "No existe una cuenta con este email",
                                HttpStatus.UNAUTHORIZED.value()
                        ));
            }

            Person foundUser = userOptional.get();

            if (foundUser.getEmailVerified() != null && !foundUser.getEmailVerified()) {
                System.out.println("[LOGIN] Usuario no verificado: " + email);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(createErrorResponse(
                                "ACCOUNT_NOT_VERIFIED",
                                "Debes verificar tu cuenta antes de iniciar sesión. Revisa tu correo electrónico.",
                                HttpStatus.FORBIDDEN.value()
                        ));
            }

            if (foundUser.getActive() != null && !foundUser.getActive()) {
                System.out.println("[LOGIN] Cuenta deshabilitada: " + email);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(createErrorResponse(
                                "ACCOUNT_DISABLED",
                                "Tu cuenta ha sido deshabilitada. Contacta a soporte.",
                                HttpStatus.FORBIDDEN.value()
                        ));
            }


            Person loginPerson = new Person();
            loginPerson.setEmail(email);
            loginPerson.setPasswordHash(rawPassword);


            System.out.println("[LOGIN] Llamando al servicio de autenticación...");
            Person authenticatedUser = authenticationService.authenticate(loginPerson);

            if (authenticatedUser == null) {
                System.out.println("[LOGIN] AuthenticationService retornó null");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse(
                                "INVALID_CREDENTIALS",
                                "Email o contraseña incorrectos",
                                HttpStatus.UNAUTHORIZED.value()
                        ));
            }

            System.out.println("[LOGIN] Usuario autenticado correctamente: " + authenticatedUser.getEmail());


            Map<String, Object> extraClaims = new HashMap<>();
            extraClaims.put("userId", authenticatedUser.getId());
            extraClaims.put("rol", authenticatedUser.getRole());

            String jwtToken = jwtService.generateToken(extraClaims, authenticatedUser);
            System.out.println("[LOGIN] Token JWT generado con userId: " + authenticatedUser.getId() + " y rol: " + authenticatedUser.getRole());


            LoginResponse loginResponse = new LoginResponse();
            loginResponse.setToken(jwtToken);
            loginResponse.setExpiresIn(jwtService.getExpirationTime());
            loginResponse.setAuthPerson(authenticatedUser);

            System.out.println("[LOGIN] Login exitoso para: " + email);
            return ResponseEntity.ok(loginResponse);

        } catch (BadCredentialsException e) {
            System.err.println("[LOGIN] BadCredentialsException: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(createErrorResponse(
                            "INVALID_CREDENTIALS",
                            "Email o contraseña incorrectos",
                            HttpStatus.UNAUTHORIZED.value()
                    ));

        } catch (UsernameNotFoundException e) {
            System.err.println("[LOGIN] Usuario no encontrado: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(createErrorResponse(
                            "USER_NOT_FOUND",
                            "No existe una cuenta con este email",
                            HttpStatus.UNAUTHORIZED.value()
                    ));

        } catch (AuthenticationException e) {
            System.err.println("[LOGIN] AuthenticationException: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(createErrorResponse(
                            "AUTHENTICATION_FAILED",
                            "Email o contraseña incorrectos",
                            HttpStatus.UNAUTHORIZED.value()
                    ));

        } catch (Exception e) {
            System.err.println("[LOGIN] Error inesperado: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse(
                            "SERVER_ERROR",
                            "Error interno del servidor. Por favor, intenta más tarde",
                            HttpStatus.INTERNAL_SERVER_ERROR.value()
                    ));
        }
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

    @PostMapping("/google/complete-registration")
    public ResponseEntity<Map<String, Object>> completeGoogleRegistration(@RequestBody Map<String, String> requestBody) {
        try {
            String code = requestBody.get("code");
            String redirectUri = requestBody.get("redirectUri");
            String role = requestBody.get("role"); // "LEARNER" o "INSTRUCTOR"

            if (code == null || code.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(createErrorResponse("Código de autorización requerido"));
            }

            if (redirectUri == null || redirectUri.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(createErrorResponse("URI de redirección requerida"));
            }

            if (role == null || (!role.equals("LEARNER") && !role.equals("INSTRUCTOR"))) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(createErrorResponse("Rol inválido. Debe ser LEARNER o INSTRUCTOR"));
            }

            System.out.println("🟢 [CompleteRegistration] Procesando registro con rol: " + role);


            Map<String, Object> tokenResponse = googleOAuthService.exchangeCodeForToken(code, redirectUri);
            String accessToken = (String) tokenResponse.get("accessToken");


            Map<String, Object> userInfo = googleOAuthService.getUserInfo(accessToken);

            Boolean verifiedEmail = (Boolean) userInfo.get("verifiedEmail");
            if (userInfo.get("email") == null || !Boolean.TRUE.equals(verifiedEmail)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(createErrorResponse("Email no verificado"));
            }

            String email = (String) userInfo.get("email");


            Optional<Person> existingPerson = personRepository.findByEmail(email);
            Person person;

            if (existingPerson.isPresent()) {
                person = existingPerson.get();
                System.out.println("✅ Usuario existente encontrado: " + email);
            } else {

                person = googleOAuthService.processGoogleUser(userInfo);
                System.out.println("✅ Nuevo usuario creado: " + email);
            }


            if (role.equals("LEARNER")) {
                if (person.getLearner() == null) {
                    Learner learner = new Learner();
                    learner.setPerson(person);
                    person.setLearner(learner);
                    personRepository.save(person);
                    System.out.println("✅ Rol LEARNER creado para: " + email);
                }
            } else if (role.equals("INSTRUCTOR")) {
                if (person.getInstructor() == null) {
                    Instructor instructor = new Instructor();
                    instructor.setPerson(person);
                    person.setInstructor(instructor);
                    personRepository.save(person);
                    System.out.println("✅ Rol INSTRUCTOR creado para: " + email);
                }
            }


            Map<String, Object> extraClaims = new HashMap<>();
            extraClaims.put("userId", person.getId());
            extraClaims.put("rol", person.getRole());

            String jwtToken = jwtService.generateToken(extraClaims, person);


            Map<String, Object> response = new HashMap<>();
            response.put("token", jwtToken);
            response.put("tokenType", "Bearer");
            response.put("expiresIn", jwtService.getExpirationTime());
            response.put("profile", createUserResponse(person));
            response.put("requiresOnboarding", true); // SIEMPRE true porque necesita seleccionar skills
            response.put("selectedRole", role);

            System.out.println(" Registro completado exitosamente para: " + email);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println(" Error en registro completo: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error al completar el registro: " + e.getMessage()));
        }
    }

    private Map<String, Object> createErrorResponse(String code, String message, int status) {
        Map<String, Object> error = new HashMap<>();
        error.put("error", true);
        error.put("code", code);
        error.put("message", message);
        error.put("status", status);
        error.put("timestamp", System.currentTimeMillis());
        return error;
    }


}