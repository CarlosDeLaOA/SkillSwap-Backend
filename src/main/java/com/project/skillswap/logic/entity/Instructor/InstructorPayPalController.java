package com.project.skillswap.logic.entity.Instructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.project.skillswap.logic.entity.Instructor.Instructor;
import com.project.skillswap.logic.entity.Instructor.InstructorPayPalService;
import com.project.skillswap.logic.entity.Instructor.InstructorRepository;
import com.project.skillswap.logic.entity.Person.Person;
import com.project.skillswap.logic.entity.Person.PersonRepository;
import com.project.skillswap.logic.entity.Transaction.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/instructor/paypal")
@CrossOrigin(origins = "*")
public class InstructorPayPalController {
    private static final Logger logger = LoggerFactory.getLogger(InstructorPayPalController.class);

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private InstructorRepository instructorRepository;

    @Autowired
    private InstructorPayPalService instructorPayPalService;

    /**
     * GET /api/instructor/paypal/info
     * Obtiene información de la cuenta PayPal del instructor
     */
    @GetMapping("/info")
    public ResponseEntity<?> getPayPalInfo(Authentication authentication) {
        try {
            String email = authentication.getName();

            Person person = personRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            Instructor instructor = person.getInstructor();
            if (instructor == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "User is not an instructor"));
            }


            Long instructorId = instructor.getId();
            Map<String, Object> info = instructorPayPalService.getPayPalInfo(instructorId);

            return ResponseEntity.ok(info);

        } catch (Exception e) {
            logger.info("[PAYPAL_CONTROLLER] Error: " + e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * POST /api/instructor/paypal/link
     * Vincula una cuenta PayPal al instructor
     */
    @PostMapping("/link")
    public ResponseEntity<?> linkPayPalAccount(
            Authentication authentication,
            @RequestBody Map<String, String> request) {
        try {
            String email = authentication.getName();
            String paypalEmail = request.get("paypalEmail");

            if (paypalEmail == null || paypalEmail.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Email de PayPal requerido"));
            }

            Person person = personRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            Instructor instructor = person.getInstructor();
            if (instructor == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "User is not an instructor"));
            }


            Long instructorId = instructor.getId();
            instructorPayPalService.linkPayPalAccount(instructorId, paypalEmail);

            return ResponseEntity.ok(Map.of(
                    "message", "Cuenta PayPal vinculada exitosamente",
                    "paypalEmail", paypalEmail
            ));

        } catch (Exception e) {
            logger.info("[PAYPAL_CONTROLLER] Error: " + e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * POST /api/instructor/paypal/withdraw
     * Procesa un retiro de SkillCoins a PayPal
     */
    @PostMapping("/withdraw")
    public ResponseEntity<?> withdrawToPayPal(
            Authentication authentication,
            @RequestBody Map<String, String> request) {
        try {
            String email = authentication.getName();
            String amountStr = request.get("amount");

            if (amountStr == null || amountStr.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Cantidad requerida"));
            }

            BigDecimal amount = new BigDecimal(amountStr);

            Person person = personRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            Instructor instructor = person.getInstructor();
            if (instructor == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "User is not an instructor"));
            }


            Long instructorId = instructor.getId();
            Transaction transaction = instructorPayPalService.withdrawToPayPal(instructorId, amount);

            return ResponseEntity.ok(Map.of(
                    "message", "Retiro procesado exitosamente",
                    "transactionId", transaction.getId(),
                    "amount", amount,
                    "payoutBatchId", transaction.getPaypalReference()
            ));

        } catch (Exception e) {
            logger.info("[PAYPAL_CONTROLLER] Error: " + e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }
}