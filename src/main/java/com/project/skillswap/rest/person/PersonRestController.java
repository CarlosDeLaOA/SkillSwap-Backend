package com.project.skillswap.rest.person;

import com.project.skillswap.logic.entity.Person.Person;
import com.project.skillswap.logic.entity.Learner.Learner;
import com.project.skillswap.logic.entity.Learner.LearnerRepository;
import com.project.skillswap.logic.entity.Instructor.Instructor;
import com.project.skillswap.logic.entity.Instructor.InstructorRepository;
import com.project.skillswap.logic.entity.Person.PersonRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/register")
@CrossOrigin(origins = "*")
public class PersonRestController {

    private final PersonRepository personRepository;
    private final LearnerRepository learnerRepository;
    private final InstructorRepository instructorRepository;
    private final PasswordEncoder passwordEncoder;

    public PersonRestController(PersonRepository personRepository,
                                LearnerRepository learnerRepository,
                                InstructorRepository instructorRepository,
                                PasswordEncoder passwordEncoder) {
        this.personRepository = personRepository;
        this.learnerRepository = learnerRepository;
        this.instructorRepository = instructorRepository;
        this.passwordEncoder = passwordEncoder;
    }

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

            List<String> categories = (List<String>) request.get("categories");
            if (categories == null || categories.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Debe seleccionar al menos una categoría de habilidad");
            }

            person.setPasswordHash(passwordEncoder.encode(person.getPasswordHash()));

            personRepository.save(person);

            Learner learner = new Learner();
            learner.setPerson(person);
            learnerRepository.save(learner);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Estudiante registrado exitosamente");
            response.put("userId", person.getId());
            response.put("email", person.getEmail());
            response.put("userType", "LEARNER");

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            System.err.println("Error en registro de learner: " + e.getMessage());
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

            List<String> categories = (List<String>) request.get("categories");
            if (categories == null || categories.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Debe seleccionar al menos una categoría de habilidad");
            }
            person.setPasswordHash(passwordEncoder.encode(person.getPasswordHash()));

            personRepository.save(person);

            Instructor instructor = new Instructor();
            instructor.setPerson(person);
            instructorRepository.save(instructor);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Instructor registrado exitosamente");
            response.put("userId", person.getId());
            response.put("email", person.getEmail());
            response.put("userType", "INSTRUCTOR");

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            System.err.println("Error en registro de instructor: " + e.getMessage());
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

    /**
     * Extrae y crea un objeto Person desde el request Map.
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

        return null; // Todo válido
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
}