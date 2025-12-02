package com.project.skillswap.logic.entity.Booking;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Service
public class BookingService {
    private static final Logger logger = LoggerFactory.getLogger(BookingService.class);

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

    @Autowired
    private SessionPaymentService sessionPaymentService;

    @Value("${app.frontend.url:http://localhost:4200}")
    private String frontendUrl;

    @Autowired(required = false)
    @Qualifier("emailExecutor")
    private Executor emailExecutor;

    /**
     * Crea un booking individual
     */
    @Transactional
    public Booking createIndividualBooking(Long sessionId, String userEmail) {

        logger.info("[BOOKING] Creando booking para email: " + userEmail);

        Person person = personRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con email: " + userEmail));

        Learner learner = person.getLearner();
        if (learner == null) {
            throw new RuntimeException("El usuario no tiene un perfil de estudiante. Por favor completa tu perfil primero.");
        }

        logger.info("[BOOKING] Learner encontrado con ID: " + learner.getId());

        LearningSession session = learningSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Sesión no encontrada con ID: " + sessionId));

        logger.info("[BOOKING] Sesión encontrada: " + session.getTitle());

        if (!SessionStatus.SCHEDULED.equals(session.getStatus()) && !SessionStatus.ACTIVE.equals(session.getStatus())) {
            throw new RuntimeException("No se puede registrar en una sesión que no está programada o activa");
        }

        if (bookingRepository.existsActiveBookingBySessionAndLearner(sessionId, learner.getId())) {
            throw new RuntimeException("Ya estás registrado en esta sesión");
        }

        long confirmedBookings = bookingRepository.countConfirmedBookingsBySessionId(sessionId);
        logger.info("[BOOKING] Cupos confirmados: " + confirmedBookings + "/" + session.getMaxCapacity());

        if (confirmedBookings >= session.getMaxCapacity()) {
            throw new RuntimeException("No hay cupos disponibles para esta sesión");
        }

        if (session.getVideoCallLink() == null || session.getVideoCallLink().isEmpty()) {
            throw new RuntimeException("La sesión no tiene un enlace de videollamada configurado");
        }

        try {
            sessionPaymentService.processSessionPayment(learner, session);
            logger.info("[BOOKING] Pago procesado exitosamente");
        } catch (IllegalStateException e) {
            logger.info("[BOOKING] Error de pago: " + e.getMessage());
            throw new RuntimeException(e.getMessage());
        } catch (Exception e) {
            logger.info("[BOOKING] Error inesperado en el pago: " + e.getMessage());
            throw new RuntimeException("Error al procesar el pago de la sesión");
        }

        List<Booking> existingBookings = bookingRepository.findByLearnerIdAndLearningSessionId(
                learner.getId(),
                sessionId
        );

        Booking booking = null;
        for (Booking existingBooking : existingBookings) {
            if (BookingStatus.CANCELLED.equals(existingBooking.getStatus())) {
                booking = existingBooking;
                booking.setStatus(BookingStatus.CONFIRMED);
                booking.setBookingDate(new Date());
                booking.setAccessLink(session.getVideoCallLink());
                logger.info("[BOOKING] Reutilizando booking cancelado previo con ID: " + booking.getId());
                break;
            }
        }

        if (booking == null) {
            booking = new Booking();
            booking.setLearningSession(session);
            booking.setLearner(learner);
            booking.setType(BookingType.INDIVIDUAL);
            booking.setStatus(BookingStatus.CONFIRMED);
            booking.setAttended(false);
            booking.setAccessLink(session.getVideoCallLink());
            logger.info("[BOOKING] Creando nuevo booking");
        }

        Booking savedBooking = bookingRepository.save(booking);
        logger.info("[BOOKING] Booking creado exitosamente con ID: " + savedBooking.getId());

        final Long bookingId = savedBooking.getId();
        final String learnerFullName = person.getFullName();
        final String learnerEmail = person.getEmail();
        final String sessionTitle = session.getTitle();
        final String sessionDescription = session.getDescription();
        final String instructorFullName = session.getInstructor().getPerson().getFullName();
        final String skillName = session.getSkill().getName();
        final String categoryName = session.getSkill().getKnowledgeArea().getName();
        final Date scheduledDatetime = session.getScheduledDatetime();
        final Integer durationMinutes = session.getDurationMinutes();
        final String videoCallLink = session.getVideoCallLink();
        final String mySessionsLink = frontendUrl + "/app/my-sessions";

        Runnable emailTask = () -> {
            try {
                bookingEmailService.sendBookingConfirmationEmail(
                        bookingId,
                        learnerFullName,
                        learnerEmail,
                        sessionTitle,
                        sessionDescription,
                        instructorFullName,
                        skillName,
                        categoryName,
                        scheduledDatetime,
                        durationMinutes,
                        videoCallLink,
                        mySessionsLink
                );
                logger.info("[BOOKING] Email de confirmación enviado a: " + learnerEmail);
            } catch (Exception e) {
                logger.error("[BOOKING] Error al enviar email: " + e.getMessage());
                e.printStackTrace();
            }
        };

        if (emailExecutor != null) {
            CompletableFuture.runAsync(emailTask, emailExecutor);
        } else {
            CompletableFuture.runAsync(emailTask);
        }

        logger.info("[BOOKING] Email de confirmación programado para envío asíncrono");

        return savedBooking;
    }

    /**
     * Crea bookings grupales para toda una comunidad (todos los miembros activos)
     */
    @Transactional
    public List<Booking> createGroupBooking(Long sessionId, Long communityId, String userEmail) {

        logger.info("[BOOKING] Creando booking grupal para comunidad: " + communityId);

        Person person = personRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con email: " + userEmail));

        Learner requestingLearner = person.getLearner();
        if (requestingLearner == null) {
            throw new RuntimeException("El usuario no tiene un perfil de estudiante");
        }

        LearningCommunity community = learningCommunityRepository.findById(communityId)
                .orElseThrow(() -> new RuntimeException("Comunidad no encontrada con ID: " + communityId));

        if (!community.getActive()) {
            throw new RuntimeException("La comunidad no está activa");
        }

        List<CommunityMember> allMembers = communityMemberRepository.findActiveMembersByCommunityId(communityId);

        if (allMembers.isEmpty()) {
            throw new RuntimeException("La comunidad no tiene miembros activos");
        }

        boolean isUserMember = allMembers.stream()
                .anyMatch(cm -> cm.getLearner().getId().equals(requestingLearner.getId()));

        if (!isUserMember) {
            throw new RuntimeException("No eres miembro de esta comunidad");
        }

        logger.info("[BOOKING] Registrando " + allMembers.size() + " miembros de la comunidad");

        LearningSession session = learningSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Sesión no encontrada con ID: " + sessionId));

        logger.info("[BOOKING] Sesión encontrada: " + session.getTitle());

        if (!SessionStatus.SCHEDULED.equals(session.getStatus()) && !SessionStatus.ACTIVE.equals(session.getStatus())) {
            throw new RuntimeException("No se puede registrar en una sesión que no está programada o activa");
        }

        if (session.getVideoCallLink() == null || session.getVideoCallLink().isEmpty()) {
            throw new RuntimeException("La sesión no tiene un enlace de videollamada configurado");
        }

        for (CommunityMember member : allMembers) {
            if (bookingRepository.existsActiveBookingBySessionAndLearner(sessionId, member.getLearner().getId())) {
                throw new RuntimeException("Uno o más miembros ya están registrados en esta sesión");
            }
        }

        long confirmedBookings = bookingRepository.countConfirmedBookingsBySessionId(sessionId);
        int availableSpots = session.getMaxCapacity() - (int) confirmedBookings;

        logger.info("[BOOKING] Cupos disponibles: " + availableSpots + " - Miembros a registrar: " + allMembers.size());

        if (availableSpots < allMembers.size()) {
            throw new RuntimeException("No hay suficientes cupos disponibles. Disponibles: " + availableSpots + ", Necesarios: " + allMembers.size());
        }

        if (session.getIsPremium() && session.getSkillcoinsCost() != null &&
                session.getSkillcoinsCost().compareTo(BigDecimal.ZERO) > 0) {

            logger.info("[BOOKING] Validando pagos para " + allMembers.size() + " miembros...");

            for (CommunityMember member : allMembers) {
                BigDecimal memberBalance = member.getLearner().getSkillcoinsBalance();
                if (memberBalance.compareTo(session.getSkillcoinsCost()) < 0) {
                    throw new RuntimeException(
                            "El miembro " + member.getLearner().getPerson().getFullName() +
                                    " no tiene SkillCoins suficientes. Necesita: " + session.getSkillcoinsCost() +
                                    ", tiene: " + memberBalance
                    );
                }
            }

            for (CommunityMember member : allMembers) {
                try {
                    sessionPaymentService.processSessionPayment(member.getLearner(), session);
                    logger.info("[BOOKING] Pago procesado para: " + member.getLearner().getPerson().getFullName());
                } catch (Exception e) {
                    logger.error("[BOOKING] Error en pago grupal: " + e.getMessage());
                    throw new RuntimeException("Error al procesar pago del grupo: " + e.getMessage());
                }
            }
        }

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

            logger.info("[BOOKING] Booking creado para learner ID: " + member.getLearner().getId());
        }

        logger.info("[BOOKING] " + createdBookings.size() + " bookings grupales creados exitosamente");

        List<Map<String, Object>> emailData = new ArrayList<>();
        for (Booking booking : createdBookings) {
            Person memberPerson = booking.getLearner().getPerson();

            Map<String, Object> data = new HashMap<>();
            data.put("bookingId", booking.getId());
            data.put("accessLink", booking.getAccessLink());
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

        Runnable emailTask = () -> {
            for (Map<String, Object> data : emailData) {
                try {
                    bookingEmailService.sendGroupBookingConfirmationEmailFromData(data);
                    logger.info("[BOOKING] Email grupal enviado a: " + data.get("personEmail"));
                } catch (Exception e) {
                    logger.error("[BOOKING] Error al enviar email a " + data.get("personEmail") + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }
        };

        if (emailExecutor != null) {
            CompletableFuture.runAsync(emailTask, emailExecutor);
        } else {
            CompletableFuture.runAsync(emailTask);
        }

        logger.info("[BOOKING] Enviando emails a " + emailData.size() + " miembros en segundo plano...");

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
    @Transactional
    public Booking joinWaitlist(Long sessionId, String userEmail) {

        logger.info("[WAITLIST] Uniendo a lista de espera - Sesión: " + sessionId);

        Person person = personRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con email: " + userEmail));

        Learner learner = person.getLearner();
        if (learner == null) {
            throw new RuntimeException("El usuario no tiene un perfil de estudiante");
        }

        LearningSession session = learningSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Sesión no encontrada con ID: " + sessionId));

        logger.info("[WAITLIST] Sesión encontrada: " + session.getTitle());

        if (!SessionStatus.SCHEDULED.equals(session.getStatus())) {
            throw new RuntimeException("No se puede unir a lista de espera de una sesión que no está programada");
        }

        List<Booking> existingBookings = bookingRepository.findByLearnerIdAndLearningSessionId(
                learner.getId(),
                sessionId
        );

        for (Booking booking : existingBookings) {
            if (BookingStatus.CONFIRMED.equals(booking.getStatus())) {
                throw new RuntimeException("Ya estás registrado en esta sesión");
            }
            if (BookingStatus.WAITING.equals(booking.getStatus())) {
                throw new RuntimeException("Ya estás en lista de espera para esta sesión");
            }
        }

        long confirmedBookings = bookingRepository.countConfirmedBookingsBySessionId(sessionId);
        int availableSpots = session.getMaxCapacity() - (int) confirmedBookings;

        if (availableSpots > 0) {
            throw new RuntimeException("Aún hay cupos disponibles. Por favor, regístrate normalmente.");
        }

        long waitlistCount = bookingRepository.countByLearningSessionIdAndStatus(sessionId, BookingStatus.WAITING);

        logger.info("[WAITLIST] Usuarios en lista de espera: " + waitlistCount);

        if (waitlistCount >= 20) {
            throw new RuntimeException("Lista de espera llena. Máximo 20 usuarios permitidos.");
        }

        Booking waitlistBooking = null;
        for (Booking booking : existingBookings) {
            if (BookingStatus.CANCELLED.equals(booking.getStatus())) {
                waitlistBooking = booking;
                waitlistBooking.setStatus(BookingStatus.WAITING);
                waitlistBooking.setBookingDate(new Date());
                waitlistBooking.setAccessLink(null);
                logger.info("[WAITLIST] Reutilizando booking CANCELLED existente");
                break;
            }
        }

        if (waitlistBooking == null) {
            waitlistBooking = new Booking();
            waitlistBooking.setLearningSession(session);
            waitlistBooking.setLearner(learner);
            waitlistBooking.setType(BookingType.INDIVIDUAL);
            waitlistBooking.setStatus(BookingStatus.WAITING);
            waitlistBooking.setAttended(false);
            waitlistBooking.setAccessLink(null);
            logger.info("[WAITLIST] Creando nuevo booking");
        }

        Booking savedBooking = bookingRepository.save(waitlistBooking);

        logger.info("[WAITLIST] Usuario agregado a lista de espera - Posición: " + (waitlistCount + 1));

        final String learnerFullName = person.getFullName();
        final String learnerEmail = person.getEmail();
        final String sessionTitle = session.getTitle();
        final String sessionDescription = session.getDescription();
        final String instructorFullName = session.getInstructor().getPerson().getFullName();
        final String skillName = session.getSkill().getName();
        final String categoryName = session.getSkill().getKnowledgeArea().getName();
        final Date scheduledDatetime = session.getScheduledDatetime();
        final Integer durationMinutes = session.getDurationMinutes();

        Runnable emailTask = () -> {
            try {
                bookingEmailService.sendWaitlistConfirmationEmail(
                        learnerFullName,
                        learnerEmail,
                        sessionTitle,
                        sessionDescription,
                        instructorFullName,
                        skillName,
                        categoryName,
                        scheduledDatetime,
                        durationMinutes
                );
                logger.info("[WAITLIST] Email de confirmación enviado a: " + learnerEmail);
            } catch (Exception e) {
                logger.error("[WAITLIST] Error al enviar email: " + e.getMessage());
                e.printStackTrace();
            }
        };

        if (emailExecutor != null) {
            CompletableFuture.runAsync(emailTask, emailExecutor);
        } else {
            CompletableFuture.runAsync(emailTask);
        }

        logger.info("[WAITLIST] Email de confirmación programado para envío asíncrono");

        return savedBooking;
    }

    /**
     * Procesa la lista de espera y convierte el primer booking WAITING a CONFIRMED
     * si hay cupos disponibles
     */
    @Transactional
    public void processWaitlist(Long sessionId) {

        logger.info("[WAITLIST] Procesando lista de espera para sesión: " + sessionId);

        LearningSession session = learningSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Sesión no encontrada"));

        long confirmedBookings = bookingRepository.countConfirmedBookingsBySessionId(sessionId);
        int availableSpots = session.getMaxCapacity() - (int) confirmedBookings;

        logger.info("[WAITLIST] Cupos disponibles: " + availableSpots);

        if (availableSpots <= 0) {
            logger.info("[WAITLIST] No hay cupos disponibles");
            return;
        }

        List<Booking> waitlist = bookingRepository
                .findByLearningSessionIdAndStatusOrderByBookingDateAsc(
                        sessionId,
                        BookingStatus.WAITING
                );

        if (waitlist.isEmpty()) {
            logger.info("[WAITLIST] No hay usuarios en lista de espera");
            return;
        }

        int spotsToFill = Math.min(availableSpots, waitlist.size());

        for (int i = 0; i < spotsToFill; i++) {
            Booking waitlistBooking = waitlist.get(i);

            waitlistBooking.setStatus(BookingStatus.CONFIRMED);
            waitlistBooking.setAccessLink(session.getVideoCallLink());
            bookingRepository.save(waitlistBooking);

            logger.info("[WAITLIST] Usuario promovido de lista de espera a confirmado: " +
                    waitlistBooking.getLearner().getPerson().getEmail());

            final String learnerFullName = waitlistBooking.getLearner().getPerson().getFullName();
            final String learnerEmail = waitlistBooking.getLearner().getPerson().getEmail();
            final String sessionTitle = session.getTitle();
            final Date scheduledDatetime = session.getScheduledDatetime();
            final Long sessionIdFinal = session.getId();

            Runnable emailTask = () -> {
                try {
                    bookingEmailService.sendSpotAvailableEmail(
                            learnerFullName,
                            learnerEmail,
                            sessionTitle,
                            scheduledDatetime,
                            sessionIdFinal
                    );
                    logger.info("[WAITLIST] Email de cupo disponible enviado a: " + learnerEmail);
                } catch (Exception e) {
                    logger.error("[WAITLIST] Error al enviar email: " + e.getMessage());
                    e.printStackTrace();
                }
            };

            if (emailExecutor != null) {
                CompletableFuture.runAsync(emailTask, emailExecutor);
            } else {
                CompletableFuture.runAsync(emailTask);
            }
        }

        logger.info("[WAITLIST] Procesamiento completado. " + spotsToFill + " usuarios promovidos.");
    }

    /**
     * Permite que un usuario salga voluntariamente de la lista de espera
     */
    @Transactional
    public void leaveWaitlist(Long bookingId, String userEmail) {

        logger.info("[WAITLIST] Usuario saliendo de lista de espera - Booking ID: " + bookingId);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking no encontrado con ID: " + bookingId));

        Person person = personRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (!booking.getLearner().getId().equals(person.getLearner().getId())) {
            throw new RuntimeException("No tienes permiso para modificar este booking");
        }

        if (!BookingStatus.WAITING.equals(booking.getStatus())) {
            throw new RuntimeException("Solo puedes salir de una lista de espera si estás en estado WAITING");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        logger.info("[WAITLIST] Usuario removido de lista de espera exitosamente");

        final String learnerFullName = person.getFullName();
        final String learnerEmail = person.getEmail();
        final String sessionTitle = booking.getLearningSession().getTitle();
        final Date scheduledDatetime = booking.getLearningSession().getScheduledDatetime();

        Runnable emailTask = () -> {
            try {
                bookingEmailService.sendWaitlistExitConfirmationEmail(
                        learnerFullName,
                        learnerEmail,
                        sessionTitle,
                        scheduledDatetime
                );
                logger.info("[WAITLIST] Email de confirmación de salida enviado a: " + learnerEmail);
            } catch (Exception e) {
                logger.error("[WAITLIST] Error al enviar email: " + e.getMessage());
                e.printStackTrace();
            }
        };

        if (emailExecutor != null) {
            CompletableFuture.runAsync(emailTask, emailExecutor);
        } else {
            CompletableFuture.runAsync(emailTask);
        }

        logger.info("[WAITLIST] Email de confirmación programado para envío asíncrono");
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

        logger.info("[BOOKING_CANCEL] Iniciando cancelación de booking: " + bookingId);

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

        LearningSession session = booking.getLearningSession();
        if (session.getStatus() == SessionStatus.ACTIVE || session.getStatus() == SessionStatus.FINISHED) {
            throw new RuntimeException("No se puede cancelar un registro de una sesión que ya inició o terminó");
        }

        boolean isGroupBooking = BookingType.GROUP.equals(booking.getType());

        if (isGroupBooking) {
            return cancelGroupBooking(booking, person, session);
        } else {
            return cancelIndividualBooking(booking, person, session);
        }
    }

    /**
     * Cancela un booking individual y procesa el reembolso si corresponde
     */
    private Booking cancelIndividualBooking(Booking booking, Person person, LearningSession session) {

        logger.info("[BOOKING_CANCEL] Cancelando booking individual: " + booking.getId());

        booking.setStatus(BookingStatus.CANCELLED);
        Booking cancelledBooking = bookingRepository.save(booking);

        logger.info("[SUCCESS] Booking individual cancelado. Cupo liberado: 1");

        try {
            sessionPaymentService.refundSessionPayment(booking.getLearner(), session);
            logger.info("[REFUND] Reembolso procesado exitosamente");
        } catch (Exception e) {
            logger.error("[REFUND] Error al procesar reembolso: " + e.getMessage());
        }

        sendCancellationNotifications(cancelledBooking, person, session, false, 1);

        logger.info("[WAITLIST] Procesando lista de espera después de cancelación...");
        try {
            processWaitlist(session.getId());
        } catch (Exception e) {
            logger.error("[WAITLIST] Error al procesar lista de espera: " + e.getMessage());
        }

        return cancelledBooking;
    }

    /**
     * Cancela todos los bookings de un grupo y procesa los reembolsos correspondientes
     */
    private Booking cancelGroupBooking(Booking booking, Person person, LearningSession session) {

        logger.info("[BOOKING_CANCEL] Cancelando booking grupal");

        LearningCommunity community = booking.getCommunity();
        if (community == null) {
            throw new RuntimeException("No se puede determinar la comunidad del booking grupal");
        }

        List<Booking> groupBookings = bookingRepository.findByLearningSessionIdAndCommunityId(
                session.getId(),
                community.getId()
        );

        int cancelledCount = 0;
        for (Booking groupBooking : groupBookings) {
            if (!BookingStatus.CANCELLED.equals(groupBooking.getStatus())) {
                groupBooking.setStatus(BookingStatus.CANCELLED);
                bookingRepository.save(groupBooking);

                try {
                    sessionPaymentService.refundSessionPayment(groupBooking.getLearner(), session);
                    logger.info("[REFUND] Reembolso grupal procesado para: " +
                            groupBooking.getLearner().getPerson().getFullName());
                } catch (Exception e) {
                    logger.error("[REFUND] Error al reembolsar a " +
                            groupBooking.getLearner().getPerson().getFullName() + ": " + e.getMessage());
                }

                cancelledCount++;
            }
        }

        logger.info("[SUCCESS] Booking grupal cancelado. Cupos liberados: " + cancelledCount);

        sendCancellationNotifications(booking, person, session, true, cancelledCount);

        logger.info("[WAITLIST] Procesando lista de espera después de cancelación grupal...");
        try {
            processWaitlist(session.getId());
        } catch (Exception e) {
            logger.error("[WAITLIST] Error al procesar lista de espera: " + e.getMessage());
        }

        return booking;
    }

    /**
     * Envía notificaciones de cancelación
     */
    private void sendCancellationNotifications(Booking booking, Person learnerPerson,
                                               LearningSession session, boolean isGroup, int spotsFreed) {
        final String learnerFullName = learnerPerson.getFullName();
        final String learnerEmail = learnerPerson.getEmail();
        final String sessionTitle = session.getTitle();
        final Date scheduledDatetime = session.getScheduledDatetime();

        final String instructorFullName = session.getInstructor().getPerson().getFullName();
        final String instructorEmail = session.getInstructor().getPerson().getEmail();
        final int maxCapacity = session.getMaxCapacity();
        final long currentBookingsCount = bookingRepository.countConfirmedBookingsBySessionId(session.getId());

        Runnable emailTask = () -> {
            try {
                bookingEmailService.sendBookingCancellationEmail(
                        learnerFullName,
                        learnerEmail,
                        sessionTitle,
                        scheduledDatetime
                );
                logger.info("[EMAIL] Confirmación de cancelación enviada a: " + learnerEmail);

                bookingEmailService.sendInstructorNotificationEmail(
                        instructorFullName,
                        instructorEmail,
                        sessionTitle,
                        scheduledDatetime,
                        learnerFullName,
                        isGroup,
                        spotsFreed,
                        maxCapacity,
                        (int) currentBookingsCount
                );
                logger.info("[EMAIL] Notificación enviada al instructor: " + instructorEmail);

            } catch (Exception e) {
                logger.error("[ERROR] Error al enviar notificaciones: " + e.getMessage());
                e.printStackTrace();
            }
        };

        if (emailExecutor != null) {
            CompletableFuture.runAsync(emailTask, emailExecutor);
        } else {
            CompletableFuture.runAsync(emailTask);
        }

        logger.info("[EMAIL] Notificaciones de cancelación programadas para envío asíncrono");
    }

}