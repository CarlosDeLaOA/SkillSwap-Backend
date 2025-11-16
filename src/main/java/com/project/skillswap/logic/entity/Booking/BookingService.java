package com.project.skillswap.logic.entity.Booking;

import com.project.skillswap.logic.entity.CommunityMember.CommunityMember;
import com.project.skillswap.logic.entity.CommunityMember.CommunityMemberRepository;
import com.project.skillswap.logic.entity.Learner.Learner;
import com.project.skillswap.logic.entity.LearningCommunity.LearningCommunity;
import com.project.skillswap.logic.entity.LearningCommunity.LearningCommunityRepository;
import com.project.skillswap.logic.entity.Person.Person;
import com.project.skillswap.logic.entity.Person.PersonRepository;
import com.project.skillswap.logic.entity.LearningSession.LearningSession;
import com.project.skillswap.logic.entity.LearningSession.LearningSessionRepository;
import com.project.skillswap.logic.entity.LearningSession.SessionStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.stream.Collectors;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.Map;
import java.util.HashMap;

@Service
public class BookingService {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private LearningSessionRepository learningSessionRepository;

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private BookingEmailService bookingEmailService;

    @Autowired
    private LearningCommunityRepository learningCommunityRepository;

    @Autowired
    private CommunityMemberRepository communityMemberRepository;

    /**
     * Crea un booking individual
     */
    @Transactional
    public Booking createIndividualBooking(Long sessionId, String userEmail) {

        System.out.println("[BOOKING] Creando booking para email: " + userEmail);

        // 1. Obtener Person por email
        Person person = personRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con email: " + userEmail));

        // 2. Obtener Learner desde Person
        Learner learner = person.getLearner();
        if (learner == null) {
            throw new RuntimeException("El usuario no tiene un perfil de estudiante. Por favor completa tu perfil primero.");
        }

        System.out.println("[BOOKING] Learner encontrado con ID: " + learner.getId());

        // 3. Validar que la sesión existe
        LearningSession session = learningSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Sesión no encontrada con ID: " + sessionId));

        System.out.println("[BOOKING] Sesión encontrada: " + session.getTitle());

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
        System.out.println("[BOOKING] Cupos confirmados: " + confirmedBookings + "/" + session.getMaxCapacity());

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
        System.out.println("[BOOKING] Booking creado exitosamente con ID: " + savedBooking.getId());

        // 8. Enviar email de confirmación 📧
        try {
            bookingEmailService.sendBookingConfirmationEmail(savedBooking, person);
            System.out.println("[BOOKING] Email de confirmación enviado a: " + person.getEmail());
        } catch (Exception e) {
            System.err.println("[BOOKING] Error al enviar email: " + e.getMessage());
            // No lanzamos excepción para que el booking se complete aunque falle el email
        }

        return savedBooking;
    }

    /**
     * Crea bookings grupales para toda una comunidad (todos los miembros activos)
     */
    @Transactional
    public List<Booking> createGroupBooking(Long sessionId, Long communityId, String userEmail) {

        System.out.println("📝 [BOOKING] Creando booking grupal para comunidad: " + communityId);

        // 1. Validar que el usuario existe y tiene perfil de learner
        Person person = personRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con email: " + userEmail));

        Learner requestingLearner = person.getLearner();
        if (requestingLearner == null) {
            throw new RuntimeException("El usuario no tiene un perfil de estudiante");
        }

        // 2. Validar que la comunidad existe
        LearningCommunity community = learningCommunityRepository.findById(communityId)
                .orElseThrow(() -> new RuntimeException("Comunidad no encontrada con ID: " + communityId));

        if (!community.getActive()) {
            throw new RuntimeException("La comunidad no está activa");
        }

        // 3. Obtener todos los miembros activos de la comunidad
        List<CommunityMember> allMembers = communityMemberRepository.findActiveMembersByCommunityId(communityId);

        if (allMembers.isEmpty()) {
            throw new RuntimeException("La comunidad no tiene miembros activos");
        }

        // 4. Validar que el usuario es miembro de la comunidad
        boolean isUserMember = allMembers.stream()
                .anyMatch(cm -> cm.getLearner().getId().equals(requestingLearner.getId()));

        if (!isUserMember) {
            throw new RuntimeException("No eres miembro de esta comunidad");
        }

        System.out.println("📊 [BOOKING] Registrando " + allMembers.size() + " miembros de la comunidad");

        // 5. Validar que la sesión existe
        LearningSession session = learningSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Sesión no encontrada con ID: " + sessionId));

        System.out.println("✅ [BOOKING] Sesión encontrada: " + session.getTitle());

        // 6. Validar que la sesión está en estado SCHEDULED
        if (!SessionStatus.SCHEDULED.equals(session.getStatus())) {
            throw new RuntimeException("No se puede registrar en una sesión que no está programada");
        }

        // 7. Validar que ningún miembro esté ya registrado
        for (CommunityMember member : allMembers) {
            if (bookingRepository.existsActiveBookingBySessionAndLearner(sessionId, member.getLearner().getId())) {
                throw new RuntimeException("Uno o más miembros ya están registrados en esta sesión");
            }
        }

        // 8. Validar que haya cupo suficiente para todos los miembros
        long confirmedBookings = bookingRepository.countConfirmedBookingsBySessionId(sessionId);
        int availableSpots = session.getMaxCapacity() - (int) confirmedBookings;

        System.out.println("📊 [BOOKING] Cupos disponibles: " + availableSpots + " - Miembros a registrar: " + allMembers.size());

        if (availableSpots < allMembers.size()) {
            throw new RuntimeException("No hay suficientes cupos disponibles. Disponibles: " + availableSpots + ", Necesarios: " + allMembers.size());
        }

        // 9. Crear bookings para todos los miembros
        List<Booking> createdBookings = new ArrayList<>();

        for (CommunityMember member : allMembers) {
            Booking booking = new Booking();
            booking.setLearningSession(session);
            booking.setLearner(member.getLearner());
            booking.setType(BookingType.GROUP);
            booking.setStatus(BookingStatus.CONFIRMED);
            booking.setAttended(false);
            booking.setCommunity(community);
            booking.setAccessLink(generateAccessLink());

            Booking savedBooking = bookingRepository.save(booking);
            createdBookings.add(savedBooking);

            System.out.println("✅ [BOOKING] Booking creado para learner ID: " + member.getLearner().getId());
        }

        System.out.println("✅ [BOOKING] " + createdBookings.size() + " bookings grupales creados exitosamente");

        // 10. Preparar datos para emails (antes de que la transacción termine)
        List<Map<String, Object>> emailData = new ArrayList<>();
        for (Booking booking : createdBookings) {
            Person memberPerson = booking.getLearner().getPerson();

            Map<String, Object> data = new HashMap<>();
            data.put("bookingId", booking.getId());
            data.put("accessLink", booking.getAccessLink());
            data.put("personEmail", memberPerson.getEmail());
            data.put("personFullName", memberPerson.getFullName());
            data.put("sessionTitle", session.getTitle());
            data.put("sessionDescription", session.getDescription());
            data.put("sessionDate", session.getScheduledDatetime());
            data.put("sessionDuration", session.getDurationMinutes());
            data.put("instructorName", session.getInstructor().getPerson().getFullName());
            data.put("skillName", session.getSkill().getName());
            data.put("categoryName", session.getSkill().getKnowledgeArea().getName());
            data.put("communityName", community.getName());

            emailData.add(data);
        }

        // 11. Enviar emails de forma asíncrona a todos los miembros
        CompletableFuture.runAsync(() -> {
            for (Map<String, Object> data : emailData) {
                try {
                    bookingEmailService.sendGroupBookingConfirmationEmailFromData(data);
                    System.out.println("📧 [BOOKING] Email grupal enviado a: " + data.get("personEmail"));
                } catch (Exception e) {
                    System.err.println("❌ [BOOKING] Error al enviar email a " + data.get("personEmail") + ": " + e.getMessage());
                }
            }
        });

        System.out.println("📧 [BOOKING] Enviando emails a " + emailData.size() + " miembros en segundo plano...");

        return createdBookings;
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
        Booking updatedBooking = bookingRepository.save(booking);

        // Enviar email de cancelación 📧
        try {
            bookingEmailService.sendBookingCancellationEmail(updatedBooking, person);
            System.out.println("📧 [BOOKING] Email de cancelación enviado a: " + person.getEmail());
        } catch (Exception e) {
            System.err.println("❌ [BOOKING] Error al enviar email: " + e.getMessage());
        }

        return updatedBooking;
    }

    /**
     * Genera un enlace único de acceso
     */
    private String generateAccessLink() {
        return "https://skillswap.com/session/join/" + UUID.randomUUID().toString();
    }
}