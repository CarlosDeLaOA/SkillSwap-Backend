package com.project.skillswap.rest.booking;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.skillswap.logic.entity.Booking.Booking;
import com.project.skillswap.logic.entity.Booking.BookingService;
import com.project.skillswap.logic.entity.Booking.BookingStatus;
import com.project.skillswap.logic.entity.Booking.BookingType;
import com.project.skillswap.logic.entity.LearningSession.LearningSession;
import com.project.skillswap.logic.entity.Learner.Learner;
import com.project.skillswap.logic.entity.auth.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@WebMvcTest(BookingRestController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("BookingRestController Unit Tests")
class BookingRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BookingService bookingService;

    @MockBean
    private JwtService jwtService;

    private String validToken;
    private String validEmail;
    private Booking mockBooking;
    private List<Booking> mockBookingList;

    @BeforeEach
    void setUp() {
        validToken = "Bearer valid.jwt.token";
        validEmail = "test@example.com";

        // Configurar mock de JwtService
        when(jwtService.extractUsername(anyString())).thenReturn(validEmail);

        // Crear booking mock
        mockBooking = new Booking();
        mockBooking.setId(1L);
        mockBooking.setType(BookingType.INDIVIDUAL);
        mockBooking.setStatus(BookingStatus.CONFIRMED);
        mockBooking.setBookingDate(new Date());
        mockBooking.setAttended(false);

        LearningSession session = new LearningSession();
        session.setId(1L);
        session.setTitle("Test Session");
        mockBooking.setLearningSession(session);

        Learner learner = new Learner();
        learner.setId(1L);
        mockBooking.setLearner(learner);

        // Crear lista de bookings mock
        mockBookingList = Arrays.asList(mockBooking);
    }

    // ==================== POST /api/bookings - Create Individual Booking ====================

    @Test
    @DisplayName("Create Individual Booking - Success")
    @WithMockUser
    void createBooking_Success() throws Exception {
        // Arrange
        Map<String, Long> request = new HashMap<>();
        request.put("learningSessionId", 1L);

        when(bookingService.createIndividualBooking(1L, validEmail)).thenReturn(mockBooking);

        // Act & Assert
        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Te has registrado exitosamente en la sesión"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"));

        verify(bookingService, times(1)).createIndividualBooking(1L, validEmail);
        verify(jwtService, times(1)).extractUsername("valid.jwt.token");
    }

    @Test
    @DisplayName("Create Individual Booking - Missing Session ID")
    @WithMockUser
    void createBooking_MissingSessionId() throws Exception {
        // Arrange
        Map<String, Long> request = new HashMap<>();
        // No se incluye learningSessionId

        // Act & Assert
        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("El ID de la sesión es requerido"));

        verify(bookingService, never()).createIndividualBooking(anyLong(), anyString());
    }

    @Test
    @DisplayName("Create Individual Booking - Service Throws Exception")
    @WithMockUser
    void createBooking_ServiceThrowsException() throws Exception {
        // Arrange
        Map<String, Long> request = new HashMap<>();
        request.put("learningSessionId", 1L);

        when(bookingService.createIndividualBooking(1L, validEmail))
                .thenThrow(new RuntimeException("Ya tienes un booking activo para esta sesión"));

        // Act & Assert
        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Ya tienes un booking activo para esta sesión"));

        verify(bookingService, times(1)).createIndividualBooking(1L, validEmail);
    }

    // ==================== GET /api/bookings/my-bookings - Get My Bookings ====================

    @Test
    @DisplayName("Get My Bookings - Success")
    @WithMockUser
    void getMyBookings_Success() throws Exception {
        // Arrange
        when(bookingService.getBookingsByUserEmail(validEmail)).thenReturn(mockBookingList);

        // Act & Assert
        mockMvc.perform(get("/api/bookings/my-bookings")
                        .header("Authorization", validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.count").value(1))
                .andExpect(jsonPath("$.data[0].id").value(1));

        verify(bookingService, times(1)).getBookingsByUserEmail(validEmail);
        verify(jwtService, times(1)).extractUsername("valid.jwt.token");
    }

    @Test
    @DisplayName("Get My Bookings - Empty List")
    @WithMockUser
    void getMyBookings_EmptyList() throws Exception {
        // Arrange
        when(bookingService.getBookingsByUserEmail(validEmail)).thenReturn(Collections.emptyList());

        // Act & Assert
        mockMvc.perform(get("/api/bookings/my-bookings")
                        .header("Authorization", validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(0)))
                .andExpect(jsonPath("$.count").value(0));

        verify(bookingService, times(1)).getBookingsByUserEmail(validEmail);
    }

    @Test
    @DisplayName("Get My Bookings - Service Throws Exception")
    @WithMockUser
    void getMyBookings_ServiceThrowsException() throws Exception {
        // Arrange
        when(bookingService.getBookingsByUserEmail(validEmail))
                .thenThrow(new RuntimeException("Error al obtener bookings"));

        // Act & Assert
        mockMvc.perform(get("/api/bookings/my-bookings")
                        .header("Authorization", validToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Error al obtener bookings"));

        verify(bookingService, times(1)).getBookingsByUserEmail(validEmail);
    }

    // ==================== PUT /api/bookings/{id}/cancel - Cancel Booking ====================

    @Test
    @DisplayName("Cancel Booking - Success")
    @WithMockUser
    void cancelBooking_Success() throws Exception {
        // Arrange
        Long bookingId = 1L;
        Booking cancelledBooking = new Booking();
        cancelledBooking.setId(bookingId);
        cancelledBooking.setStatus(BookingStatus.CANCELLED);

        when(bookingService.cancelBooking(bookingId, validEmail)).thenReturn(cancelledBooking);

        // Act & Assert
        mockMvc.perform(put("/api/bookings/{id}/cancel", bookingId)
                        .header("Authorization", validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Registro cancelado exitosamente"))
                .andExpect(jsonPath("$.data.id").value(bookingId))
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));

        verify(bookingService, times(1)).cancelBooking(bookingId, validEmail);
        verify(jwtService, times(1)).extractUsername("valid.jwt.token");
    }

    @Test
    @DisplayName("Cancel Booking - IllegalArgumentException")
    @WithMockUser
    void cancelBooking_IllegalArgumentException() throws Exception {
        // Arrange
        Long bookingId = 1L;
        when(bookingService.cancelBooking(bookingId, validEmail))
                .thenThrow(new IllegalArgumentException("No tienes permiso para cancelar este booking"));

        // Act & Assert
        mockMvc.perform(put("/api/bookings/{id}/cancel", bookingId)
                        .header("Authorization", validToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("No tienes permiso para cancelar este booking"));

        verify(bookingService, times(1)).cancelBooking(bookingId, validEmail);
    }

    @Test
    @DisplayName("Cancel Booking - RuntimeException")
    @WithMockUser
    void cancelBooking_RuntimeException() throws Exception {
        // Arrange
        Long bookingId = 1L;
        when(bookingService.cancelBooking(bookingId, validEmail))
                .thenThrow(new RuntimeException("Booking no encontrado"));

        // Act & Assert
        mockMvc.perform(put("/api/bookings/{id}/cancel", bookingId)
                        .header("Authorization", validToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Booking no encontrado"));

        verify(bookingService, times(1)).cancelBooking(bookingId, validEmail);
    }



    @Test
    @DisplayName("Create Group Booking - Success")
    @WithMockUser
    void createGroupBooking_Success() throws Exception {
        // Arrange
        Map<String, Long> request = new HashMap<>();
        request.put("learningSessionId", 1L);
        request.put("communityId", 1L);

        Booking groupBooking1 = new Booking();
        groupBooking1.setId(1L);
        groupBooking1.setType(BookingType.GROUP);
        groupBooking1.setStatus(BookingStatus.CONFIRMED);

        Booking groupBooking2 = new Booking();
        groupBooking2.setId(2L);
        groupBooking2.setType(BookingType.GROUP);
        groupBooking2.setStatus(BookingStatus.CONFIRMED);

        List<Booking> groupBookings = Arrays.asList(groupBooking1, groupBooking2);

        when(bookingService.createGroupBooking(1L, 1L, validEmail)).thenReturn(groupBookings);

        // Act & Assert
        mockMvc.perform(post("/api/bookings/group")
                        .header("Authorization", validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value(containsString("Se ha registrado exitosamente al grupo")))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.count").value(2));

        verify(bookingService, times(1)).createGroupBooking(1L, 1L, validEmail);
    }

    @Test
    @DisplayName("Create Group Booking - Missing Session ID")
    @WithMockUser
    void createGroupBooking_MissingSessionId() throws Exception {
        // Arrange
        Map<String, Long> request = new HashMap<>();
        request.put("communityId", 1L);
        // Falta learningSessionId

        // Act & Assert
        mockMvc.perform(post("/api/bookings/group")
                        .header("Authorization", validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("El ID de la sesión es requerido"));

        verify(bookingService, never()).createGroupBooking(anyLong(), anyLong(), anyString());
    }

    @Test
    @DisplayName("Create Group Booking - Missing Community ID")
    @WithMockUser
    void createGroupBooking_MissingCommunityId() throws Exception {
        // Arrange
        Map<String, Long> request = new HashMap<>();
        request.put("learningSessionId", 1L);
        // Falta communityId

        // Act & Assert
        mockMvc.perform(post("/api/bookings/group")
                        .header("Authorization", validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("El ID de la comunidad es requerido"));

        verify(bookingService, never()).createGroupBooking(anyLong(), anyLong(), anyString());
    }

    @Test
    @DisplayName("Create Group Booking - Service Throws Exception")
    @WithMockUser
    void createGroupBooking_ServiceThrowsException() throws Exception {
        // Arrange
        Map<String, Long> request = new HashMap<>();
        request.put("learningSessionId", 1L);
        request.put("communityId", 1L);

        when(bookingService.createGroupBooking(1L, 1L, validEmail))
                .thenThrow(new RuntimeException("No tienes permiso para crear booking grupal"));

        // Act & Assert
        mockMvc.perform(post("/api/bookings/group")
                        .header("Authorization", validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("No tienes permiso para crear booking grupal"));

        verify(bookingService, times(1)).createGroupBooking(1L, 1L, validEmail);
    }

    // ==================== POST /api/bookings/waitlist - Join Waitlist ====================

    @Test
    @DisplayName("Join Waitlist - Success")
    @WithMockUser
    void joinWaitlist_Success() throws Exception {
        // Arrange
        Map<String, Long> request = new HashMap<>();
        request.put("learningSessionId", 1L);

        Booking waitlistBooking = new Booking();
        waitlistBooking.setId(1L);
        waitlistBooking.setStatus(BookingStatus.WAITING);

        when(bookingService.joinWaitlist(1L, validEmail)).thenReturn(waitlistBooking);

        // Act & Assert
        mockMvc.perform(post("/api/bookings/waitlist")
                        .header("Authorization", validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Te has unido exitosamente a la lista de espera"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.status").value("WAITING"));

        verify(bookingService, times(1)).joinWaitlist(1L, validEmail);
    }

    @Test
    @DisplayName("Join Waitlist - Missing Session ID")
    @WithMockUser
    void joinWaitlist_MissingSessionId() throws Exception {
        // Arrange
        Map<String, Long> request = new HashMap<>();
        // No se incluye learningSessionId

        // Act & Assert
        mockMvc.perform(post("/api/bookings/waitlist")
                        .header("Authorization", validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("El ID de la sesión es requerido"));

        verify(bookingService, never()).joinWaitlist(anyLong(), anyString());
    }

    @Test
    @DisplayName("Join Waitlist - Service Throws Exception")
    @WithMockUser
    void joinWaitlist_ServiceThrowsException() throws Exception {
        // Arrange
        Map<String, Long> request = new HashMap<>();
        request.put("learningSessionId", 1L);

        when(bookingService.joinWaitlist(1L, validEmail))
                .thenThrow(new RuntimeException("Ya estás en la lista de espera"));

        // Act & Assert
        mockMvc.perform(post("/api/bookings/waitlist")
                        .header("Authorization", validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Ya estás en la lista de espera"));

        verify(bookingService, times(1)).joinWaitlist(1L, validEmail);
    }

    // ==================== POST /api/bookings/process-waitlist/{sessionId} ====================

    @Test
    @DisplayName("Process Waitlist - Success")
    @WithMockUser
    void processWaitlist_Success() throws Exception {
        // Arrange
        Long sessionId = 1L;
        doNothing().when(bookingService).processWaitlist(sessionId);

        // Act & Assert
        mockMvc.perform(post("/api/bookings/process-waitlist/{sessionId}", sessionId)
                        .header("Authorization", validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Lista de espera procesada exitosamente"));

        verify(bookingService, times(1)).processWaitlist(sessionId);
    }

    @Test
    @DisplayName("Process Waitlist - Service Throws Exception")
    @WithMockUser
    void processWaitlist_ServiceThrowsException() throws Exception {
        // Arrange
        Long sessionId = 1L;
        doThrow(new RuntimeException("No hay bookings en lista de espera"))
                .when(bookingService).processWaitlist(sessionId);

        // Act & Assert
        mockMvc.perform(post("/api/bookings/process-waitlist/{sessionId}", sessionId)
                        .header("Authorization", validToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("No hay bookings en lista de espera"));

        verify(bookingService, times(1)).processWaitlist(sessionId);
    }

    // ==================== DELETE /api/bookings/waitlist/{bookingId} ====================

    @Test
    @DisplayName("Leave Waitlist - Success")
    @WithMockUser
    void leaveWaitlist_Success() throws Exception {
        // Arrange
        Long bookingId = 1L;
        doNothing().when(bookingService).leaveWaitlist(bookingId, validEmail);

        // Act & Assert
        mockMvc.perform(delete("/api/bookings/waitlist/{bookingId}", bookingId)
                        .header("Authorization", validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Has salido exitosamente de la lista de espera"));

        verify(bookingService, times(1)).leaveWaitlist(bookingId, validEmail);
        verify(jwtService, times(1)).extractUsername("valid.jwt.token");
    }

    @Test
    @DisplayName("Leave Waitlist - Service Throws Exception")
    @WithMockUser
    void leaveWaitlist_ServiceThrowsException() throws Exception {
        // Arrange
        Long bookingId = 1L;
        doThrow(new RuntimeException("Booking no encontrado"))
                .when(bookingService).leaveWaitlist(bookingId, validEmail);

        // Act & Assert
        mockMvc.perform(delete("/api/bookings/waitlist/{bookingId}", bookingId)
                        .header("Authorization", validToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Booking no encontrado"));

        verify(bookingService, times(1)).leaveWaitlist(bookingId, validEmail);
    }

    // ==================== Authorization Tests ====================

    @Test
    @DisplayName("Create Booking - Missing Authorization Header")
    @WithMockUser
    void createBooking_MissingAuthorizationHeader() throws Exception {
        // Arrange
        Map<String, Long> request = new HashMap<>();
        request.put("learningSessionId", 1L);

        // Act & Assert
        // En un test con @WithMockUser y addFilters = false, la petición debe fallar
        // porque el controlador espera el header Authorization
        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}