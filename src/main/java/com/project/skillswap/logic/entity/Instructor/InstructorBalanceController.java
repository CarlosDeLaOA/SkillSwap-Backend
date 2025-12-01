package com.project.skillswap.logic.entity.Instructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.project.skillswap.logic.entity.Instructor.Instructor;
import com.project.skillswap.logic.entity.Instructor.InstructorRepository;
import com.project.skillswap.logic.entity.Person.Person;
import com.project.skillswap.logic.entity.Person.PersonRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/instructor")
@CrossOrigin(origins = "*")
public class InstructorBalanceController {
    private static final Logger logger = LoggerFactory.getLogger(InstructorBalanceController.class);

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private InstructorRepository instructorRepository;

    /**
     * GET /api/instructor/balance
     * Obtiene el balance de SkillCoins del instructor
     */
    @GetMapping("/balance")
    public ResponseEntity<?> getBalance(Authentication authentication) {
        try {
            String email = authentication.getName();

            Person person = personRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            Instructor instructor = person.getInstructor();
            if (instructor == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "User is not an instructor"));
            }

            Map<String, Object> response = new HashMap<>();
            response.put("balance", instructor.getSkillcoinsBalance());
            response.put("totalEarnings", instructor.getTotalEarnings());
            response.put("sessionsTaught", instructor.getSessionsTaught());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.info("[INSTRUCTOR_BALANCE] Error: " + e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }
}