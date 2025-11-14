package com.project.skillswap.rest.person;

import com.project.skillswap.logic.entity.Person.Person;
import com.project.skillswap.logic.entity.Learner.Learner;
import com.project.skillswap.logic.entity.Learner.LearnerRepository;
import com.project.skillswap.logic.entity.Instructor.Instructor;
import com.project.skillswap.logic.entity.Instructor.InstructorRepository;
import com.project.skillswap.logic.entity.Person.PersonRepository;
import com.project.skillswap.logic.entity.UserSkill.UserSkillService;
import com.project.skillswap.logic.entity.Skill.Skill;
import com.project.skillswap.logic.entity.Skill.SkillRepository;
import com.project.skillswap.logic.entity.Knowledgearea.KnowledgeAreaRepository;
import com.project.skillswap.logic.entity.verification.VerificationService;
import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Controlador REST para gestionar el registro de usuarios.
 * Versión mejorada con mejor manejo de errores de correo electrónico.
 */
@RestController
@RequestMapping("/register")
@CrossOrigin(origins = "*")
public class PersonRestController {

    //#region Dependencies
    private final PersonRepository personRepository;
    private final LearnerRepository learnerRepository;
    private final InstructorRepository instructorRepository;
    private final PasswordEncoder passwordEncoder;
    private final VerificationService verificationService;
    private final UserSkillService userSkillService;
    private final SkillRepository skillRepository;
    private final KnowledgeAreaRepository knowledgeAreaRepository;

    @Value("${app.development.mode:false}")
    private boolean developmentMode;

    public PersonRestController(PersonRepository personRepository,
                                LearnerRepository learnerRepository,
                                InstructorRepository instructorRepository,
                                PasswordEncoder passwordEncoder,
                                VerificationService verificationService,
                                UserSkillService userSkillService,
                                SkillRepository skillRepository,
                                KnowledgeAreaRepository knowledgeAreaRepository) {
        this.personRepository = personRepository;
        this.learnerRepository = learnerRepository;
        this.instructorRepository = instructorRepository;
        this.passwordEncoder = passwordEncoder;
        this.verificationService = verificationService;
        this.userSkillService = userSkillService;
        this.skillRepository = skillRepository;
        this.knowledgeAreaRepository = knowledgeAreaRepository;
    }
    //#endregion

    //#region Endpoints
    /**
     * Registra un nuevo usuario como Learner (estudiante).
     *
     * @param request datos del registro
     * @return respuesta con el usuario creado o mensaje de error
     */
    @PostMapping("/learner")
    public ResponseEntity<?> registerLearner(@RequestBody Map<String, Object> request) {
        try {
            String role = (String) request.get("role");
            if (role == null || !role.equalsIgnoreCase("LEARNER")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Debe seleccionar el rol de LEARNER para este registro");
            }

            Person person = extractPersonFromRequest(request);
            ResponseEntity<?> validationError = validatePersonData(person);
            if (validationError != null) {
                return validationError;
            }

            // ✅ Ahora esperamos skillIds en lugar de categories
            List<Integer> skillIdsRaw = (List<Integer>) request.get("skillIds");
            if (skillIdsRaw == null || skillIdsRaw.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Debe seleccionar al menos una habilidad");
            }

            // Convertir a Long (el repositorio espera Long IDs)
            List<Long> skillIds = skillIdsRaw.stream()
                    .map(Integer::longValue)
                    .toList();

            person.setPasswordHash(passwordEncoder.encode(person.getPasswordHash()));

            person.setEmailVerified(developmentMode);
            personRepository.save(person);

            Learner learner = new Learner();
            learner.setPerson(person);
            learnerRepository.save(learner);

            try {
                userSkillService.saveUserSkills(person, skillIds);
            } catch (Exception e) {
                System.err.println("⚠️ Error guardando skills del usuario: " + e.getMessage());
            }

            boolean emailSent = false;
            if (!developmentMode) {
                try {
                    verificationService.createAndSendVerificationToken(person);
                    emailSent = true;
                } catch (Exception e) {
                    System.err.println("⚠️ Error enviando correo: " + e.getMessage());
                }
            }

            Map<String, Object> response = new HashMap<>();
            if (developmentMode) {
                response.put("message", "Estudiante registrado exitosamente (modo desarrollo - email auto-verificado)");
            } else if (emailSent) {
                response.put("message", "Estudiante registrado exitosamente. Por favor verifica tu correo electrónico");
            } else {
                response.put("message", "Estudiante registrado exitosamente. Nota: no se pudo enviar el correo de verificación.");
                response.put("emailWarning", true);
            }

            response.put("userId", person.getId());
            response.put("email", person.getEmail());
            response.put("userType", "LEARNER");
            response.put("emailVerified", person.getEmailVerified());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            System.err.println("❌ Error en registro de learner: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al procesar el registro. Por favor intente nuevamente.");
        }
    }

    /**
     * Registra un nuevo usuario como Instructor.
     *
     * @param request datos del registro
     * @return respuesta con el usuario creado o mensaje de error
     */
    @PostMapping("/instructor")
    public ResponseEntity<?> registerInstructor(@RequestBody Map<String, Object> request) {
        try {
            String role = (String) request.get("role");
            if (role == null || !role.equalsIgnoreCase("INSTRUCTOR")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Debe seleccionar el rol de INSTRUCTOR para este registro");
            }

            Person person = extractPersonFromRequest(request);
            ResponseEntity<?> validationError = validatePersonData(person);
            if (validationError != null) {
                return validationError;
            }

            List<Integer> skillIdsRaw = (List<Integer>) request.get("skillIds");
            if (skillIdsRaw == null || skillIdsRaw.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Debe seleccionar al menos una habilidad");
            }

            List<Long> skillIds = skillIdsRaw.stream()
                    .map(Integer::longValue)
                    .toList();

            person.setPasswordHash(passwordEncoder.encode(person.getPasswordHash()));

            person.setEmailVerified(developmentMode);
            personRepository.save(person);

            Instructor instructor = new Instructor();
            instructor.setPerson(person);
            instructorRepository.save(instructor);

            try {
                userSkillService.saveUserSkills(person, skillIds);
            } catch (Exception e) {
                System.err.println("⚠️ Error guardando skills del usuario: " + e.getMessage());
            }

            boolean emailSent = false;
            if (!developmentMode) {
                try {
                    verificationService.createAndSendVerificationToken(person);
                    emailSent = true;
                } catch (Exception e) {
                    System.err.println("⚠️ Error enviando correo: " + e.getMessage());
                }
            }

            Map<String, Object> response = new HashMap<>();
            if (developmentMode) {
                response.put("message", "Instructor registrado exitosamente (modo desarrollo - email auto-verificado)");
            } else if (emailSent) {
                response.put("message", "Instructor registrado exitosamente. Por favor verifica tu correo electrónico");
            } else {
                response.put("message", "Instructor registrado exitosamente. Nota: no se pudo enviar el correo de verificación.");
                response.put("emailWarning", true);
            }

            response.put("userId", person.getId());
            response.put("email", person.getEmail());
            response.put("userType", "INSTRUCTOR");
            response.put("emailVerified", person.getEmailVerified());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            System.err.println("❌ Error en registro de instructor: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al procesar el registro. Por favor intente nuevamente.");
        }
    }

    /**
     * Verifica si un correo electrónico está disponible para el registro.
     *
     * @param email el correo a verificar
     * @return respuesta indicando si el correo está disponible
     */
    @GetMapping("/check-email")
    public ResponseEntity<?> checkEmail(@RequestParam String email) {
        boolean emailExists = personRepository.findByEmail(email).isPresent();
        Map<String, Object> response = new HashMap<>();
        response.put("email", email);
        response.put("available", !emailExists);

        if (emailExists) {
            response.put("message", "El correo electrónico ya está registrado");
        }

        return ResponseEntity.ok(response);
    }
    //#endregion

    //#region Private Methods
    /**
     * Extrae y crea un objeto Person desde el request Map.
     *
     * @param request mapa con los datos del request
     * @return objeto Person creado
     */
    private Person extractPersonFromRequest(Map<String, Object> request) {
        Person person = new Person();
        person.setEmail((String) request.get("email"));
        person.setPasswordHash((String) request.get("password"));
        person.setFullName((String) request.get("fullName"));
        person.setProfilePhotoUrl((String) request.get("profilePhotoUrl"));
        person.setPreferredLanguage((String) request.get("preferredLanguage"));
        return person;
    }

    /**
     * Valida todos los datos de Person antes de guardar.
     *
     * @param person objeto Person a validar
     * @return ResponseEntity con error o null si todo está bien
     */
    private ResponseEntity<?> validatePersonData(Person person) {

        if (person.getEmail() == null || person.getEmail().trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("El correo electrónico es requerido");
        }

        if (!person.getEmail().matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("El formato del correo electrónico no es válido");
        }

        Optional<Person> existingPerson = personRepository.findByEmail(person.getEmail());
        if (existingPerson.isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("El correo electrónico ya está registrado. Por favor use otro correo.");
        }

        if (person.getFullName() == null || person.getFullName().trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("El nombre completo es requerido");
        }

        if (person.getPasswordHash() == null || person.getPasswordHash().trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("La contraseña es requerida");
        }

        String passwordValidationResult = validatePassword(person.getPasswordHash());
        if (!passwordValidationResult.equals("valid")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(passwordValidationResult);
        }

        return null;
    }

    /**
     * Valida que la contraseña cumpla con los requisitos mínimos de seguridad.
     *
     * @param password la contraseña en texto claro
     * @return mensaje detallado de errores o "valid" si cumple todos los requisitos
     */
    private String validatePassword(String password) {
        StringBuilder message = new StringBuilder();

        if (password.length() < 8) {
            message.append("La contraseña debe tener al menos 8 caracteres. ");
        }
        if (!password.matches(".*[A-Z].*")) {
            message.append("La contraseña debe contener al menos una letra mayúscula. ");
        }
        if (!password.matches(".*[a-z].*")) {
            message.append("La contraseña debe contener al menos una letra minúscula. ");
        }
        if (!password.matches(".*\\d.*")) {
            message.append("La contraseña debe contener al menos un número. ");
        }
        if (!password.matches(".*[@$!%*?&].*")) {
            message.append("La contraseña debe contener al menos un carácter especial (@, $, !, %, *, ?, &). ");
        }

        return message.length() == 0 ? "valid" : message.toString().trim();
    }

    /**
     * Encuentra los IDs de skills basándose en las categorías seleccionadas
     *
     * @param categories lista de nombres de knowledge areas
     * @return lista de skill IDs
     */
    private List<Long> findSkillIdsByCategories(List<String> categories) {
        return categories.stream()
                .flatMap(categoryName ->
                        knowledgeAreaRepository.findByName(categoryName)
                                .map(knowledgeArea ->
                                        skillRepository.findActiveSkillsByKnowledgeAreaId(knowledgeArea.getId())
                                                .stream()
                                                .map(Skill::getId)
                                )
                                .orElse(java.util.stream.Stream.empty())
                )
                .collect(Collectors.toList());
    }
    //#endregion
}