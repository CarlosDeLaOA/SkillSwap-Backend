package com.project.skillswap.logic.entity.Booking;

import com.project.skillswap.logic.entity.Learner.Learner;
import com.project.skillswap.logic.entity.Person.Person;
import com.project.skillswap.logic.entity.Person.PersonRepository;
import com.project.skillswap.logic.entity.LearningSession.LearningSession;
import com.project.skillswap.logic.entity.LearningSession.LearningSessionRepository;
import com.project.skillswap.logic.entity.LearningSession.SessionStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class BookingService {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private LearningSessionRepository learningSessionRepository;

    @Autowired
    private PersonRepository personRepository;

    /**
     * Crea un booking individual
     */
    @Transactional
    public Booking createIndividualBooking(Long sessionId, String userEmail) {

        System.out.println("📝 [BOOKING] Creando booking para email: " + userEmail);

        // 1. Obtener Person por email
        Person person = personRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con email: " + userEmail));

        // 2. Obtener Learner desde Person
        Learner learner = person.getLearner();
        if (learner == null) {
            throw new RuntimeException("El usuario no tiene un perfil de estudiante. Por favor completa tu perfil primero.");
        }

        System.out.println("✅ [BOOKING] Learner encontrado con ID: " + learner.getId());

        // 3. Validar que la sesión existe
        LearningSession session = learningSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Sesión no encontrada con ID: " + sessionId));

        System.out.println("✅ [BOOKING] Sesión encontrada: " + session.getTitle());

        // 4. Validar que la sesión está en estado SCHEDULED
        if (!SessionStatus.SCHEDULED.equals(session.getStatus())) {
            throw new RuntimeException("No se puede registrar en una sesión que no está programada");
        }

        // 5. Validar que el usuario no esté ya registrado
        if (bookingRepository.existsActiveBookingBySessionAndLearner(sessionId, learner.getId())) {
            throw new RuntimeException("Ya estás registrado en esta sesión");
        }

        // 6. Validar que haya cupo disponible
        long confirmedBookings = bookingRepository.countConfirmedBookingsBySessionId(sessionId);
        System.out.println("📊 [BOOKING] Cupos confirmados: " + confirmedBookings + "/" + session.getMaxCapacity());

        if (confirmedBookings >= session.getMaxCapacity()) {
            throw new RuntimeException("No hay cupos disponibles para esta sesión");
        }

        // 7. Crear el booking
        Booking booking = new Booking();
        booking.setLearningSession(session);
        booking.setLearner(learner);
        booking.setType(BookingType.INDIVIDUAL);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setAttended(false);
        booking.setAccessLink(generateAccessLink());

        Booking savedBooking = bookingRepository.save(booking);
        System.out.println("✅ [BOOKING] Booking creado exitosamente con ID: " + savedBooking.getId());

        return savedBooking;
    }

    /**
     * Obtiene todos los bookings de un usuario por email
     */
    @Transactional(readOnly = true)
    public List<Booking> getBookingsByUserEmail(String email) {
        Person person = personRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con email: " + email));

        Learner learner = person.getLearner();
        if (learner == null) {
            throw new RuntimeException("El usuario no tiene un perfil de estudiante");
        }

        return bookingRepository.findByLearnerId(learner.getId());
    }

    /**
     * Cancela un booking
     */
    @Transactional
    public Booking cancelBooking(Long bookingId, String userEmail) {

        Person person = personRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Learner learner = person.getLearner();
        if (learner == null) {
            throw new RuntimeException("El usuario no tiene un perfil de estudiante");
        }

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking no encontrado"));

        if (!booking.getLearner().getId().equals(learner.getId())) {
            throw new RuntimeException("No tienes permiso para cancelar este booking");
        }

        if (BookingStatus.CANCELLED.equals(booking.getStatus())) {
            throw new RuntimeException("Este booking ya está cancelado");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        return bookingRepository.save(booking);
    }

    /**
     * Genera un enlace único de acceso
     */
    private String generateAccessLink() {
        return "https://skillswap.com/session/join/" + UUID.randomUUID().toString();
    }
}