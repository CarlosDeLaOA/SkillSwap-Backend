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

import java.util.*;
import java.util.stream.Collectors;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.CompletableFuture;

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

        // 1.Obtener Person por email
        Person person = personRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con email: " + userEmail));

        // 2.Obtener Learner desde Person
        Learner learner = person.getLearner();
        if (learner == null) {
            throw new RuntimeException("El usuario no tiene un perfil de estudiante. Por favor completa tu perfil primero.");
        }

        System.out.println("[BOOKING] Learner encontrado con ID: " + learner.getId());

        // 3.Validar que la sesión existe
        LearningSession session = learningSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Sesión no encontrada con ID: " + sessionId));

        System.out.println("[BOOKING] Sesión encontrada: " + session.getTitle());

        // 4. Validar que la sesión está en estado SCHEDULED o ACTIVE
        if (! SessionStatus.SCHEDULED.equals(session.getStatus()) && !SessionStatus.ACTIVE.equals(session.getStatus())) {
            throw new RuntimeException("No se puede registrar en una sesión que no está programada o activa");
        }

        // 5.Validar que el usuario no esté ya registrado
        if (bookingRepository.existsActiveBookingBySessionAndLearner(sessionId, learner.getId())) {
            throw new RuntimeException("Ya estás registrado en esta sesión");
        }

        // 6.Validar que haya cupo disponible
        long confirmedBookings = bookingRepository.countConfirmedBookingsBySessionId(sessionId);
        System.out.println("[BOOKING] Cupos confirmados: " + confirmedBookings + "/" + session.getMaxCapacity());

        if (confirmedBookings >= session.getMaxCapacity()) {
            throw new RuntimeException("No hay cupos disponibles para esta sesión");
        }

        // 7.Validar que la sesión tenga un video_call_link configurado
        if (session.getVideoCallLink() == null || session.getVideoCallLink().isEmpty()) {
            throw new RuntimeException("La sesión no tiene un enlace de videollamada configurado");
        }

        // 8. Crear el booking
        Booking booking = new Booking();
        booking.setLearningSession(session);
        booking.setLearner(learner);
        booking.setType(BookingType.INDIVIDUAL);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setAttended(false);
        booking.setAccessLink(session.getVideoCallLink());

        Booking savedBooking = bookingRepository.save(booking);
        System.out.println("[BOOKING] Booking creado exitosamente con ID: " + savedBooking.getId());

        // 9.Enviar email de confirmación con el video_call_link
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

        System.out.println("[BOOKING] Creando booking grupal para comunidad: " + communityId);

        // 1.Validar que el usuario existe y tiene perfil de learner
        Person person = personRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con email: " + userEmail));

        Learner requestingLearner = person.getLearner();
        if (requestingLearner == null) {
            throw new RuntimeException("El usuario no tiene un perfil de estudiante");
        }

        // 2.Validar que la comunidad existe
        LearningCommunity community = learningCommunityRepository.findById(communityId)
                .orElseThrow(() -> new RuntimeException("Comunidad no encontrada con ID: " + communityId));

        if (! community.getActive()) {
            throw new RuntimeException("La comunidad no está activa");
        }

        // 3.Obtener todos los miembros activos de la comunidad
        List<CommunityMember> allMembers = communityMemberRepository.findActiveMembersByCommunityId(communityId);

        if (allMembers.isEmpty()) {
            throw new RuntimeException("La comunidad no tiene miembros activos");
        }

        // 4.Validar que el usuario es miembro de la comunidad
        boolean isUserMember = allMembers.stream()
                .anyMatch(cm -> cm.getLearner().getId().equals(requestingLearner.getId()));

        if (!isUserMember) {
            throw new RuntimeException("No eres miembro de esta comunidad");
        }

        System.out.println("[BOOKING] Registrando " + allMembers.size() + " miembros de la comunidad");

        // 5.Validar que la sesión existe
        LearningSession session = learningSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Sesión no encontrada con ID: " + sessionId));

        System.out.println("[BOOKING] Sesión encontrada: " + session.getTitle());

        // 6. Validar que la sesión está en estado SCHEDULED o ACTIVE
        if (!SessionStatus.SCHEDULED.equals(session.getStatus()) && !SessionStatus.ACTIVE.equals(session.getStatus())) {
            throw new RuntimeException("No se puede registrar en una sesión que no está programada o activa");
        }

        // 7.Validar que la sesión tenga un video_call_link configurado
        if (session.getVideoCallLink() == null || session.getVideoCallLink().isEmpty()) {
            throw new RuntimeException("La sesión no tiene un enlace de videollamada configurado");
        }

        // 8.Validar que ningún miembro esté ya registrado
        for (CommunityMember member : allMembers) {
            if (bookingRepository.existsActiveBookingBySessionAndLearner(sessionId, member.getLearner().getId())) {
                throw new RuntimeException("Uno o más miembros ya están registrados en esta sesión");
            }
        }

        // 9.Validar que haya cupo suficiente para todos los miembros
        long confirmedBookings = bookingRepository.countConfirmedBookingsBySessionId(sessionId);
        int availableSpots = session.getMaxCapacity() - (int) confirmedBookings;

        System.out.println("[BOOKING] Cupos disponibles: " + availableSpots + " - Miembros a registrar: " + allMembers.size());

        if (availableSpots < allMembers.size()) {
            throw new RuntimeException("No hay suficientes cupos disponibles. Disponibles: " + availableSpots + ", Necesarios: " + allMembers.size());
        }

        // 10.Crear bookings para todos los miembros
        List<Booking> createdBookings = new ArrayList<>();

        for (CommunityMember member : allMembers) {
            Booking booking = new Booking();
            booking.setLearningSession(session);
            booking.setLearner(member.getLearner());
            booking.setType(BookingType.GROUP);
            booking.setStatus(BookingStatus.CONFIRMED);
            booking.setAttended(false);
            booking.setCommunity(community);
            booking.setAccessLink(session.getVideoCallLink());

            Booking savedBooking = bookingRepository.save(booking);
            createdBookings.add(savedBooking);

            System.out.println("[BOOKING] Booking creado para learner ID: " + member.getLearner().getId());
        }

        System.out.println("[BOOKING] " + createdBookings.size() + " bookings grupales creados exitosamente");

        // 11.Preparar datos para emails (antes de que la transacción termine)
        List<Map<String, Object>> emailData = new ArrayList<>();
        for (Booking booking : createdBookings) {
            Person memberPerson = booking.getLearner().getPerson();

            Map<String, Object> data = new HashMap<>();
            data.put("bookingId", booking.getId());
            data.put("accessLink", session.getVideoCallLink());
            data.put("videoCallLink", session.getVideoCallLink());
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

        // 12.Enviar emails de forma asíncrona a todos los miembros
        CompletableFuture.runAsync(() -> {
            for (Map<String, Object> data : emailData) {
                try {
                    bookingEmailService.sendGroupBookingConfirmationEmailFromData(data);
                    System.out.println("[BOOKING] Email grupal enviado a: " + data.get("personEmail"));
                } catch (Exception e) {
                    System.err.println("[BOOKING] Error al enviar email a " + data.get("personEmail") + ": " + e.getMessage());
                }
            }
        });

        System.out.println("[BOOKING] Enviando emails a " + emailData.size() + " miembros en segundo plano...");

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
     * Genera un enlace único de acceso
     * se mantiene solo para compatibilidad con lista de espera
     */
    private String generateAccessLink() {
        return "https://skillswap.com/session/join/" + UUID.randomUUID().toString();
    }

    /**
     * Une a un usuario a la lista de espera de una sesión
     */
    /**
     * Une a un usuario a la lista de espera de una sesión
     */
    @Transactional
    public Booking joinWaitlist(Long sessionId, String userEmail) {

        System.out.println("[WAITLIST] Uniendo a lista de espera - Sesión: " + sessionId);

        // 1.Validar que el usuario existe y tiene perfil de learner
        Person person = personRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con email: " + userEmail));

        Learner learner = person.getLearner();
        if (learner == null) {
            throw new RuntimeException("El usuario no tiene un perfil de estudiante");
        }

        // 2.Validar que la sesión existe
        LearningSession session = learningSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Sesión no encontrada con ID: " + sessionId));

        System.out.println("[WAITLIST] Sesión encontrada: " + session.getTitle());

        // 3.Validar que la sesión está en estado SCHEDULED
        if (!SessionStatus.SCHEDULED.equals(session.getStatus())) {
            throw new RuntimeException("No se puede unir a lista de espera de una sesión que no está programada");
        }

        // 4.Buscar si ya existe un booking para este usuario y sesión
        List<Booking> existingBookings = bookingRepository.findByLearnerIdAndLearningSessionId(
                learner.getId(),
                sessionId
        );

        // Verificar si tiene un booking activo (CONFIRMED o WAITING)
        for (Booking booking : existingBookings) {
            if (BookingStatus.CONFIRMED.equals(booking.getStatus())) {
                throw new RuntimeException("Ya estás registrado en esta sesión");
            }
            if (BookingStatus.WAITING.equals(booking.getStatus())) {
                throw new RuntimeException("Ya estás en lista de espera para esta sesión");
            }
        }

        // 5. Verificar que la sesión realmente está llena
        long confirmedBookings = bookingRepository.countConfirmedBookingsBySessionId(sessionId);
        int availableSpots = session.getMaxCapacity() - (int) confirmedBookings;

        if (availableSpots > 0) {
            throw new RuntimeException("Aún hay cupos disponibles. Por favor, regístrate normalmente.");
        }

        // 6.Contar cuántos usuarios hay en lista de espera
        long waitlistCount = bookingRepository.countByLearningSessionIdAndStatus(sessionId, BookingStatus.WAITING);

        System.out.println("[WAITLIST] Usuarios en lista de espera: " + waitlistCount);

        if (waitlistCount >= 20) {
            throw new RuntimeException("Lista de espera llena.Máximo 20 usuarios permitidos.");
        }

        // 7.Buscar si existe un booking CANCELLED que podamos reutilizar
        Booking waitlistBooking = null;
        for (Booking booking : existingBookings) {
            if (BookingStatus.CANCELLED.equals(booking.getStatus())) {
                // Reutilizar el booking existente
                waitlistBooking = booking;
                waitlistBooking.setStatus(BookingStatus.WAITING);
                waitlistBooking.setBookingDate(new Date());
                waitlistBooking.setAccessLink(null);
                System.out.println("[WAITLIST] Reutilizando booking CANCELLED existente");
                break;
            }
        }

        // 8.Si no existe booking previo, crear uno nuevo
        if (waitlistBooking == null) {
            waitlistBooking = new Booking();
            waitlistBooking.setLearningSession(session);
            waitlistBooking.setLearner(learner);
            waitlistBooking.setType(BookingType.INDIVIDUAL);
            waitlistBooking.setStatus(BookingStatus.WAITING);
            waitlistBooking.setAttended(false);
            waitlistBooking.setAccessLink(null);
            System.out.println("[WAITLIST] Creando nuevo booking");
        }

        Booking savedBooking = bookingRepository.save(waitlistBooking);

        System.out.println("[WAITLIST] Usuario agregado a lista de espera - Posición: " + (waitlistCount + 1));

        // 9.Enviar email de confirmación de lista de espera
        try {
            bookingEmailService.sendWaitlistConfirmationEmail(savedBooking, person);
            System.out.println("[WAITLIST] Email de confirmación enviado");
        } catch (Exception e) {
            System.err.println("[WAITLIST] Error al enviar email: " + e.getMessage());
        }

        return savedBooking;
    }

    /**
     * Procesa la lista de espera y convierte el primer booking WAITING a CONFIRMED
     * si hay cupos disponibles
     */
    @Transactional
    public void processWaitlist(Long sessionId) {

        System.out.println("[WAITLIST] Procesando lista de espera para sesión: " + sessionId);

        // 1.Obtener la sesión
        LearningSession session = learningSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Sesión no encontrada"));

        // 2.Verificar cupos disponibles
        long confirmedBookings = bookingRepository.countConfirmedBookingsBySessionId(sessionId);
        int availableSpots = session.getMaxCapacity() - (int) confirmedBookings;

        System.out.println("[WAITLIST] Cupos disponibles: " + availableSpots);

        if (availableSpots <= 0) {
            System.out.println("[WAITLIST] No hay cupos disponibles");
            return;
        }

        // 3.Obtener lista de espera ordenada por fecha
        List<Booking> waitlist = bookingRepository
                .findByLearningSessionIdAndStatusOrderByBookingDateAsc(
                        sessionId,
                        BookingStatus.WAITING
                );

        if (waitlist.isEmpty()) {
            System.out.println("[WAITLIST] No hay usuarios en lista de espera");
            return;
        }

        // 4.Procesar solo los primeros N usuarios (según cupos disponibles)
        int spotsToFill = Math.min(availableSpots, waitlist.size());

        for (int i = 0; i < spotsToFill; i++) {
            Booking waitlistBooking = waitlist.get(i);

            // Cambiar estado a CONFIRMED y usar el video_call_link de la sesión
            waitlistBooking.setStatus(BookingStatus.CONFIRMED);
            waitlistBooking.setAccessLink(session.getVideoCallLink());
            bookingRepository.save(waitlistBooking);

            System.out.println("[WAITLIST] Usuario promovido de lista de espera a confirmado: " +
                    waitlistBooking.getLearner().getPerson().getEmail());

            // Enviar email de notificación
            try {
                Person person = waitlistBooking.getLearner().getPerson();
                bookingEmailService.sendSpotAvailableEmail(waitlistBooking, person);
                System.out.println("[WAITLIST] Email de cupo disponible enviado");
            } catch (Exception e) {
                System.err.println("[WAITLIST] Error al enviar email: " + e.getMessage());
            }
        }

        System.out.println("[WAITLIST] Procesamiento completado. " + spotsToFill + " usuarios promovidos.");
    }


    /**
     * Permite que un usuario salga voluntariamente de la lista de espera
     */
    @Transactional
    public void leaveWaitlist(Long bookingId, String userEmail) {

        System.out.println("[WAITLIST] Usuario saliendo de lista de espera - Booking ID: " + bookingId);

        // 1.Buscar el booking
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking no encontrado con ID: " + bookingId));

        // 2.Validar que el booking pertenece al usuario
        Person person = personRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (!booking.getLearner().getId().equals(person.getLearner().getId())) {
            throw new RuntimeException("No tienes permiso para modificar este booking");
        }

        // 3.Validar que el booking está en estado WAITING
        if (!BookingStatus.WAITING.equals(booking.getStatus())) {
            throw new RuntimeException("Solo puedes salir de una lista de espera si estás en estado WAITING");
        }

        // 4.Cambiar estado a CANCELLED
        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        System.out.println("[WAITLIST] Usuario removido de lista de espera exitosamente");

        // 5.Enviar email de confirmación de salida
        try {
            bookingEmailService.sendWaitlistExitConfirmationEmail(booking, person);
            System.out.println("[WAITLIST] Email de confirmación de salida enviado");
        } catch (Exception e) {
            System.err.println("[WAITLIST] Error al enviar email: " + e.getMessage());
        }
    }

    /**
     * Cancela un booking individual o grupal
     *
     * @param bookingId ID del booking a cancelar
     * @param userEmail Email del usuario que cancela
     * @return Booking cancelado
     */
    @Transactional
    public Booking cancelBooking(Long bookingId, String userEmail) {

        System.out.println("[BOOKING_CANCEL] Iniciando cancelación de booking: " + bookingId);

        // 1.Validar usuario
        Person person = personRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Learner learner = person.getLearner();
        if (learner == null) {
            throw new RuntimeException("El usuario no tiene un perfil de estudiante");
        }

        // 2.Obtener booking
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking no encontrado"));

        // 3.Validar permisos
        if (!booking.getLearner().getId().equals(learner.getId())) {
            throw new RuntimeException("No tienes permiso para cancelar este booking");
        }

        // 4.Validar que no esté ya cancelado
        if (BookingStatus.CANCELLED.equals(booking.getStatus())) {
            throw new RuntimeException("Este booking ya está cancelado");
        }

        // 5. Validar que la sesión no haya iniciado
        LearningSession session = booking.getLearningSession();
        if (session.getStatus() == SessionStatus.ACTIVE || session.getStatus() == SessionStatus.FINISHED) {
            throw new RuntimeException("No se puede cancelar un registro de una sesión que ya inició o terminó");
        }

        // 6.Determinar si es cancelación individual o grupal
        boolean isGroupBooking = BookingType.GROUP.equals(booking.getType());

        if (isGroupBooking) {
            return cancelGroupBooking(booking, person, session);
        } else {
            return cancelIndividualBooking(booking, person, session);
        }
    }

    /**
     * Cancela un booking individual
     */
    private Booking cancelIndividualBooking(Booking booking, Person person, LearningSession session) {

        System.out.println("[BOOKING_CANCEL] Cancelando booking individual: " + booking.getId());

        // Cancelar el booking
        booking.setStatus(BookingStatus.CANCELLED);
        Booking cancelledBooking = bookingRepository.save(booking);

        System.out.println("[SUCCESS] Booking individual cancelado. Cupo liberado: 1");

        // Enviar notificaciones
        sendCancellationNotifications(cancelledBooking, person, session, false, 1);

        // Procesar lista de espera automáticamente
        System.out.println("[WAITLIST] Procesando lista de espera después de cancelación...");
        try {
            processWaitlist(session.getId());
        } catch (Exception e) {
            System.err.println("[WAITLIST] Error al procesar lista de espera: " + e.getMessage());
            // No lanzamos excepción para que la cancelación se complete
        }

        return cancelledBooking;
    }

    /**
     * Cancela todos los bookings de un grupo
     */
    private Booking cancelGroupBooking(Booking booking, Person person, LearningSession session) {

        System.out.println("[BOOKING_CANCEL] Cancelando booking grupal");

        LearningCommunity community = booking.getCommunity();
        if (community == null) {
            throw new RuntimeException("No se puede determinar la comunidad del booking grupal");
        }

        // Obtener todos los bookings del grupo para esta sesión
        List<Booking> groupBookings = bookingRepository.findByLearningSessionIdAndCommunityId(
                session.getId(),
                community.getId()
        );

        int cancelledCount = 0;
        for (Booking groupBooking : groupBookings) {
            if (! BookingStatus.CANCELLED.equals(groupBooking.getStatus())) {
                groupBooking.setStatus(BookingStatus.CANCELLED);
                bookingRepository.save(groupBooking);
                cancelledCount++;
            }
        }

        System.out.println("[SUCCESS] Booking grupal cancelado. Cupos liberados: " + cancelledCount);

        // Enviar notificaciones
        sendCancellationNotifications(booking, person, session, true, cancelledCount);

        // Procesar lista de espera automáticamente
        System.out.println("[WAITLIST] Procesando lista de espera después de cancelación grupal...");
        try {
            processWaitlist(session.getId());
        } catch (Exception e) {
            System.err.println("[WAITLIST] Error al procesar lista de espera: " + e.getMessage());
            // No lanzamos excepción para que la cancelación se complete
        }

        return booking;
    }

    /**
     * Envía notificaciones de cancelación
     */
    private void sendCancellationNotifications(Booking booking, Person learnerPerson,
                                               LearningSession session, boolean isGroup, int spotsFreed) {
        try {
            // 1.Email de confirmación al learner
            bookingEmailService.sendBookingCancellationEmail(booking, learnerPerson);
            System.out.println("[EMAIL] Confirmación de cancelación enviada a: " + learnerPerson.getEmail());

            // 2.Notificar al instructor
            Person instructorPerson = session.getInstructor().getPerson();
            bookingEmailService.sendInstructorNotificationEmail(
                    session,
                    instructorPerson,
                    learnerPerson.getFullName(),
                    isGroup,
                    spotsFreed
            );
            System.out.println("[EMAIL] Notificación enviada al instructor: " + instructorPerson.getEmail());

        } catch (Exception e) {
            System.err.println("[ERROR] Error al enviar notificaciones: " + e.getMessage());
            // No lanzamos excepción para que la cancelación se complete
        }
    }
}