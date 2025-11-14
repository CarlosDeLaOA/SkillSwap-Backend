package com.project.skillswap.rest.booking;

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
            System.out.println("📥 [BOOKING] POST /api/bookings");

            String token = authHeader.replace("Bearer ", "");
            String userEmail = jwtService.extractUsername(token); // 👈 CAMBIAR AQUÍ

            System.out.println("👤 [BOOKING] Usuario autenticado: " + userEmail);

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
            System.err.println("❌ [BOOKING] Error: " + e.getMessage());
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
            String userEmail = jwtService.extractUsername(token); // 👈 CAMBIAR AQUÍ

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
            String token = authHeader.replace("Bearer ", "");
            String userEmail = jwtService.extractUsername(token); // 👈 CAMBIAR AQUÍ

            Booking booking = bookingService.cancelBooking(id, userEmail);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Booking cancelado exitosamente");
            response.put("data", booking);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }
}