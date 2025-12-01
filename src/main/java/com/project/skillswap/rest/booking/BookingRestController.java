
package com.project.skillswap.rest.booking;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import com.project.skillswap.logic.entity.Booking.Booking;
import com.project.skillswap.logic.entity.Booking.BookingService;
import com.project.skillswap.logic.entity.auth.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bookings")
@CrossOrigin(origins = "*")
public class BookingRestController {
    private static final Logger logger = LoggerFactory.getLogger(BookingRestController.class);

    @Autowired
    private BookingService bookingService;

    @Autowired
    private JwtService jwtService;

    /**
     * POST /api/bookings
     * Crea un nuevo booking individual
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createBooking(
            @RequestBody Map<String, Long> request,
            @RequestHeader("Authorization") String authHeader) {
        try {
            logger.info("[BOOKING] POST /api/bookings");

            String token = authHeader.replace("Bearer ", "");
            String userEmail = jwtService.extractUsername(token);

            logger.info("[BOOKING] Usuario autenticado: " + userEmail);

            Long sessionId = request.get("learningSessionId");
            if (sessionId == null) {
                throw new RuntimeException("El ID de la sesión es requerido");
            }

            Booking booking = bookingService.createIndividualBooking(sessionId, userEmail);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Te has registrado exitosamente en la sesión");
            response.put("data", booking);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            logger.info("[BOOKING] Error: " + e.getMessage());
            e.printStackTrace();

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    @GetMapping("/my-bookings")
    public ResponseEntity<Map<String, Object>> getMyBookings(
            @RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            String userEmail = jwtService.extractUsername(token);

            List<Booking> bookings = bookingService.getBookingsByUserEmail(userEmail);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", bookings);
            response.put("count", bookings.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<Map<String, Object>> cancelBooking(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {
        try {
            logger.info(" [BOOKING] PUT /api/bookings/" + id + "/cancel");

            String token = authHeader.replace("Bearer ", "");
            String userEmail = jwtService.extractUsername(token);

            logger.info("[BOOKING] Usuario autenticado: " + userEmail);
            logger.info("[BOOKING] Cancelando booking ID: " + id);

            Booking booking = bookingService.cancelBooking(id, userEmail);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Registro cancelado exitosamente");
            response.put("data", booking);

            logger.info("[BOOKING] Booking cancelado exitosamente");

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            logger.info("[BOOKING] Error de validación: " + e.getMessage());

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);

        } catch (RuntimeException e) {
            logger.info("[BOOKING] Error de ejecución: " + e.getMessage());

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);

        } catch (Exception e) {
            logger.info("[BOOKING] Error inesperado: " + e.getMessage());
            e.printStackTrace();

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error interno del servidor");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    /**
     * POST /api/bookings/group
     * Crea un booking grupal para una comunidad
     */
    @PostMapping("/group")
    public ResponseEntity<Map<String, Object>> createGroupBooking(
            @RequestBody Map<String, Long> request,
            @RequestHeader("Authorization") String authHeader) {
        try {
            logger.info("[BOOKING] POST /api/bookings/group");

            String token = authHeader.replace("Bearer ", "");
            String userEmail = jwtService.extractUsername(token);

            logger.info("[BOOKING] Usuario autenticado: " + userEmail);

            Long sessionId = request.get("learningSessionId");
            Long communityId = request.get("communityId");

            if (sessionId == null) {
                throw new RuntimeException("El ID de la sesión es requerido");
            }

            if (communityId == null) {
                throw new RuntimeException("El ID de la comunidad es requerido");
            }

            List<Booking> bookings = bookingService.createGroupBooking(sessionId, communityId, userEmail);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Se ha registrado exitosamente al grupo en la sesión (" + bookings.size() + " miembros)");
            response.put("data", bookings);
            response.put("count", bookings.size());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            logger.info("[BOOKING] Error: " + e.getMessage());
            e.printStackTrace();

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    /**
     * POST /api/bookings/waitlist
     * Une a un usuario a la lista de espera de una sesión
     */
    @PostMapping("/waitlist")
    public ResponseEntity<Map<String, Object>> joinWaitlist(
            @RequestBody Map<String, Long> request,
            @RequestHeader("Authorization") String authHeader) {
        try {
            logger.info("[WAITLIST] POST /api/bookings/waitlist");

            String token = authHeader.replace("Bearer ", "");
            String userEmail = jwtService.extractUsername(token);

            logger.info("[WAITLIST] Usuario autenticado: " + userEmail);

            Long sessionId = request.get("learningSessionId");

            if (sessionId == null) {
                throw new RuntimeException("El ID de la sesión es requerido");
            }

            Booking waitlistBooking = bookingService.joinWaitlist(sessionId, userEmail);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Te has unido exitosamente a la lista de espera");
            response.put("data", waitlistBooking);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            logger.info("[WAITLIST] Error: " + e.getMessage());
            e.printStackTrace();

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }


    /**
     * POST /api/bookings/process-waitlist/{sessionId}
     * Procesa la lista de espera manualmente (para testing)
     */
    @PostMapping("/process-waitlist/{sessionId}")
    public ResponseEntity<Map<String, Object>> processWaitlist(
            @PathVariable Long sessionId,
            @RequestHeader("Authorization") String authHeader) {
        try {
            logger.info("[WAITLIST] POST /api/bookings/process-waitlist/" + sessionId);

            bookingService.processWaitlist(sessionId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Lista de espera procesada exitosamente");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.info("[WAITLIST] Error: " + e.getMessage());
            e.printStackTrace();

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }


    /**
     * DELETE /api/bookings/waitlist/{bookingId}
     * Permite al usuario salir voluntariamente de la lista de espera
     */
    @DeleteMapping("/waitlist/{bookingId}")
    public ResponseEntity<Map<String, Object>> leaveWaitlist(
            @PathVariable Long bookingId,
            @RequestHeader("Authorization") String authHeader) {
        try {
            logger.info("[WAITLIST] DELETE /api/bookings/waitlist/" + bookingId);

            String token = authHeader.replace("Bearer ", "");
            String userEmail = jwtService.extractUsername(token);

            logger.info("[WAITLIST] Usuario autenticado: " + userEmail);

            bookingService.leaveWaitlist(bookingId, userEmail);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Has salido exitosamente de la lista de espera");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.info("[WAITLIST] Error: " + e.getMessage());
            e.printStackTrace();

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }



}

