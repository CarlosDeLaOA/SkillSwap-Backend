package com.project.skillswap.logic.entity.Booking;

import com.project.skillswap.logic.entity.CommunityMember.CommunityMember;
import com.project.skillswap.logic.entity.CommunityMember.CommunityMemberRepository;
import com.project.skillswap.logic.entity.Instructor.Instructor;
import com.project.skillswap.logic.entity.Instructor.InstructorRepository;
import com.project.skillswap.logic.entity.Learner.Learner;
import com.project.skillswap.logic.entity.Learner.LearnerRepository;
import com.project.skillswap.logic.entity.LearningCommunity.LearningCommunity;
import com.project.skillswap.logic.entity.LearningCommunity.LearningCommunityRepository;
import com.project.skillswap.logic.entity.Person.Person;
import com.project.skillswap.logic.entity.Person.PersonRepository;
import com.project.skillswap.logic.entity.LearningSession.LearningSession;
import com.project.skillswap.logic.entity.LearningSession.LearningSessionRepository;
import com.project.skillswap.logic.entity.LearningSession.SessionStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
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

    @Autowired
    private LearnerRepository learnerRepository;

    @Autowired
    private InstructorRepository instructorRepository;

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

        // 3. Validar que la sesión existe
        LearningSession session = learningSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Sesión no encontrada con ID: " + sessionId));

        System.out.println("[BOOKING] Sesión encontrada: " + session.getTitle());

        // 4.Validar que la sesión está en estado SCHEDULED o ACTIVE
        if (! SessionStatus.SCHEDULED.equals(session.getStatus()) && ! SessionStatus.ACTIVE.equals(session.getStatus())) {
            throw new RuntimeException("No se puede registrar en una sesión que no está programada o activa");
        }

        // 5. Buscar bookings existentes para este usuario y sesión
        List<Booking> existingBookings = bookingRepository.findByLearnerIdAndLearningSessionId(learner.getId(), sessionId);

        // Verificar si ya tiene un booking activo
        for (Booking existingBooking : existingBookings) {
            if (BookingStatus.CONFIRMED.equals(existingBooking.getStatus())) {
                throw new RuntimeException("Ya estás registrado en esta sesión");
            }
            if (BookingStatus.WAITING.equals(existingBooking.getStatus())) {
                throw new RuntimeException("Ya estás en lista de espera para esta sesión");
            }
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

        // 8.Validar y procesar pago de SkillCoins para sesiones Premium
        if (Boolean.TRUE.equals(session.getIsPremium())) {
            processSkillCoinsPayment(learner, session);
        }

        // 9. Buscar si existe un booking CANCELLED que podamos reutilizar
        Booking booking = null;
        for (Booking existingBooking : existingBookings) {
            if (BookingStatus.CANCELLED.equals(existingBooking.getStatus())) {
                booking = existingBooking;
                System.out.println("[BOOKING] Reutilizando booking CANCELLED existente con ID: " + booking.getId());
                break;
            }
        }

        // 10.Si no existe booking previo, crear uno nuevo
        if (booking == null) {
            booking = new Booking();
            booking.setLearningSession(session);
            booking.setLearner(learner);
            System.out.println("[BOOKING] Creando nuevo booking");
        }

        // 11. Actualizar datos del booking
        booking.setType(BookingType.INDIVIDUAL);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setAttended(false);
        booking.setAccessLink(session.getVideoCallLink());

        Booking savedBooking = bookingRepository.save(booking);
        System.out.println("[BOOKING] Booking guardado exitosamente con ID: " + savedBooking.getId());

        // 12.Enviar email de confirmación
        try {
            bookingEmailService.sendBookingConfirmationEmail(savedBooking, person);
            System.out.println("[BOOKING] Email de confirmación enviado a: " + person.getEmail());
        } catch (Exception e) {
            System.err.println("[BOOKING] Error al enviar email: " + e.getMessage());
        }

        return savedBooking;
    }

    /**
     * Procesa el pago de SkillCoins para sesiones Premium
     */
    private void processSkillCoinsPayment(Learner learner, LearningSession session) {
        BigDecimal cost = session.getSkillcoinsCost();

        if (cost == null || cost.compareTo(BigDecimal.ZERO) <= 0) {
            System.out.println("[SKILLCOINS] Sesión premium sin costo definido, permitiendo registro gratuito");
            return;
        }

        BigDecimal learnerBalance = learner.getSkillcoinsBalance();
        if (learnerBalance == null) {
            learnerBalance = BigDecimal.ZERO;
        }

        System.out.println("[SKILLCOINS] Procesando pago - Costo: " + cost + ", Balance del estudiante: " + learnerBalance);

        // Validar balance suficiente
        if (learnerBalance.compareTo(cost) < 0) {
            throw new RuntimeException(
                    String.format("Saldo insuficiente de SkillCoins.Necesitas %s SkillCoins pero solo tienes %s. " +
                                    "Por favor, adquiere más SkillCoins para inscribirte en esta sesión premium.",
                            cost.intValue(), learnerBalance.intValue())
            );
        }

        // Debitar del estudiante
        BigDecimal newLearnerBalance = learnerBalance.subtract(cost);
        learner.setSkillcoinsBalance(newLearnerBalance);
        learnerRepository.save(learner);

        System.out.println("[SKILLCOINS] Debitado del estudiante. Nuevo balance: " + newLearnerBalance);

        // Acreditar al instructor
        Instructor instructor = session.getInstructor();
        BigDecimal instructorBalance = instructor.getSkillcoinsBalance();
        if (instructorBalance == null) {
            instructorBalance = BigDecimal.ZERO;
        }

        BigDecimal newInstructorBalance = instructorBalance.add(cost);
        instructor.setSkillcoinsBalance(newInstructorBalance);
        instructorRepository.save(instructor);

        System.out.println("[SKILLCOINS] Acreditado al instructor " + instructor.getPerson().getFullName() +
                ".Nuevo balance: " + newInstructorBalance);

        System.out.println("[SKILLCOINS] Transacción completada exitosamente. Transferidos " + cost + " SkillCoins");
    }

    /**
     * Valida que el estudiante tenga suficiente balance para una sesión Premium
     */
    private void validateSkillCoinsBalance(Learner learner, LearningSession session) {
        BigDecimal cost = session.getSkillcoinsCost();

        if (cost == null || cost.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        BigDecimal balance = learner.getSkillcoinsBalance();
        if (balance == null) {
            balance = BigDecimal.ZERO;
        }

        if (balance.compareTo(cost) < 0) {
            throw new RuntimeException(
                    String.format("Saldo insuficiente de SkillCoins para unirse a la lista de espera." +
                                    "Esta sesión cuesta %s SkillCoins y solo tienes %s.",
                            cost.intValue(), balance.intValue())
            );
        }

        System.out.println("[SKILLCOINS] Balance validado para lista de espera. Tiene: " + balance + ", Costo: " + cost);
    }

    /**
     * Reembolsa SkillCoins al estudiante cuando cancela una sesión Premium
     */
    private void refundSkillCoins(Learner learner, LearningSession session) {
        BigDecimal cost = session.getSkillcoinsCost();

        if (cost == null || cost.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        System.out.println("[SKILLCOINS] Procesando reembolso de " + cost + " SkillCoins");

        // Debitar del instructor
        Instructor instructor = session.getInstructor();
        BigDecimal instructorBalance = instructor.getSkillcoinsBalance();
        if (instructorBalance == null) {
            instructorBalance = BigDecimal.ZERO;
        }

        // Solo reembolsar si el instructor tiene fondos suficientes
        if (instructorBalance.compareTo(cost) >= 0) {
            BigDecimal newInstructorBalance = instructorBalance.subtract(cost);
            instructor.setSkillcoinsBalance(newInstructorBalance);
            instructorRepository.save(instructor);

            // Acreditar al estudiante
            BigDecimal learnerBalance = learner.getSkillcoinsBalance();
            if (learnerBalance == null) {
                learnerBalance = BigDecimal.ZERO;
            }

            BigDecimal newLearnerBalance = learnerBalance.add(cost);
            learner.setSkillcoinsBalance(newLearnerBalance);
            learnerRepository.save(learner);

            System.out.println("[SKILLCOINS] Reembolso completado.Estudiante nuevo balance: " + newLearnerBalance +
                    ", Instructor nuevo balance: " + newInstructorBalance);
        } else {
            System.err.println("[SKILLCOINS] El instructor no tiene fondos suficientes para reembolsar. " +
                    "Balance: " + instructorBalance + ", Costo: " + cost);
        }
    }
    /**
     * Crea bookings grupales para toda una comunidad
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

        // 8. Validar que ningún miembro esté ya registrado (CONFIRMED o WAITING)
        for (CommunityMember member : allMembers) {
            List<Booking> memberBookings = bookingRepository.findByLearnerIdAndLearningSessionId(
                    member.getLearner().getId(), sessionId);
            for (Booking booking : memberBookings) {
                if (BookingStatus.CONFIRMED.equals(booking.getStatus()) || BookingStatus.WAITING.equals(booking.getStatus())) {
                    throw new RuntimeException("Uno o más miembros ya están registrados en esta sesión");
                }
            }
        }

        // 9. Validar que haya cupo suficiente para todos los miembros
        long confirmedBookings = bookingRepository.countConfirmedBookingsBySessionId(sessionId);
        int availableSpots = session.getMaxCapacity() - (int) confirmedBookings;

        System.out.println("[BOOKING] Cupos disponibles: " + availableSpots + " - Miembros a registrar: " + allMembers.size());

        if (availableSpots < allMembers.size()) {
            throw new RuntimeException("No hay suficientes cupos disponibles. Disponibles: " + availableSpots + ", Necesarios: " + allMembers.size());
        }

        // 10. Validar y procesar pagos de SkillCoins para sesiones Premium
        if (Boolean.TRUE.equals(session.getIsPremium())) {
            processGroupSkillCoinsPayment(allMembers, session);
        }

        // 11. Crear bookings para todos los miembros (reutilizando cancelados si existen)
        List<Booking> createdBookings = new ArrayList<>();

        for (CommunityMember member : allMembers) {
            // Buscar si existe un booking CANCELLED para reutilizar
            List<Booking> memberBookings = bookingRepository.findByLearnerIdAndLearningSessionId(
                    member.getLearner().getId(), sessionId);

            Booking booking = null;
            for (Booking existingBooking : memberBookings) {
                if (BookingStatus.CANCELLED.equals(existingBooking.getStatus())) {
                    booking = existingBooking;
                    System.out.println("[BOOKING] Reutilizando booking CANCELLED para learner ID: " + member.getLearner().getId());
                    break;
                }
            }

            if (booking == null) {
                booking = new Booking();
                booking.setLearningSession(session);
                booking.setLearner(member.getLearner());
            }

            booking.setType(BookingType.GROUP);
            booking.setStatus(BookingStatus.CONFIRMED);
            booking.setAttended(false);
            booking.setCommunity(community);
            booking.setAccessLink(session.getVideoCallLink());

            Booking savedBooking = bookingRepository.save(booking);
            createdBookings.add(savedBooking);

            System.out.println("[BOOKING] Booking guardado para learner ID: " + member.getLearner().getId());
        }

        System.out.println("[BOOKING] " + createdBookings.size() + " bookings grupales creados exitosamente");

        // 12.Preparar datos para emails
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

        // 13.Enviar emails de forma asíncrona
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
     * Procesa el pago de SkillCoins para bookings grupales
     */
    private void processGroupSkillCoinsPayment(List<CommunityMember> members, LearningSession session) {
        BigDecimal costPerPerson = session.getSkillcoinsCost();

        if (costPerPerson == null || costPerPerson.compareTo(BigDecimal.ZERO) <= 0) {
            System.out.println("[SKILLCOINS] Sesión premium sin costo definido, permitiendo registro gratuito");
            return;
        }

        System.out.println("[SKILLCOINS] Procesando pago grupal - Costo por persona: " + costPerPerson +
                ", Miembros: " + members.size());

        // Primero validar que TODOS tengan suficiente balance
        List<String> membersWithInsufficientFunds = new ArrayList<>();

        for (CommunityMember member : members) {
            Learner learner = member.getLearner();
            BigDecimal balance = learner.getSkillcoinsBalance();
            if (balance == null) {
                balance = BigDecimal.ZERO;
            }

            if (balance.compareTo(costPerPerson) < 0) {
                membersWithInsufficientFunds.add(
                        learner.getPerson().getFullName() + " (tiene " + balance.intValue() + " SkillCoins)"
                );
            }
        }

        if (!membersWithInsufficientFunds.isEmpty()) {
            throw new RuntimeException(
                    "Los siguientes miembros no tienen suficientes SkillCoins (" + costPerPerson.intValue() + " requeridos): " +
                            String.join(", ", membersWithInsufficientFunds)
            );
        }

        // Procesar pagos
        BigDecimal totalToInstructor = BigDecimal.ZERO;

        for (CommunityMember member : members) {
            Learner learner = member.getLearner();
            BigDecimal currentBalance = learner.getSkillcoinsBalance();
            BigDecimal newBalance = currentBalance.subtract(costPerPerson);
            learner.setSkillcoinsBalance(newBalance);
            learnerRepository.save(learner);

            totalToInstructor = totalToInstructor.add(costPerPerson);

            System.out.println("[SKILLCOINS] Debitado de " + learner.getPerson().getFullName() +
                    ". Nuevo balance: " + newBalance);
        }

        // Acreditar al instructor
        Instructor instructor = session.getInstructor();
        BigDecimal instructorBalance = instructor.getSkillcoinsBalance();
        if (instructorBalance == null) {
            instructorBalance = BigDecimal.ZERO;
        }

        BigDecimal newInstructorBalance = instructorBalance.add(totalToInstructor);
        instructor.setSkillcoinsBalance(newInstructorBalance);
        instructorRepository.save(instructor);

        System.out.println("[SKILLCOINS] Transacción grupal completada. Total transferido al instructor: " + totalToInstructor + " SkillCoins");
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
     */
    private String generateAccessLink() {
        return "https://skillswap.com/session/join/" + UUID.randomUUID().toString();
    }
    /**
     * Une a un usuario a la lista de espera de una sesión
     */
    @Transactional
    public Booking joinWaitlist(Long sessionId, String userEmail) {

        System.out.println("[WAITLIST] Uniendo a lista de espera - Sesión: " + sessionId);

        Person person = personRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con email: " + userEmail));

        Learner learner = person.getLearner();
        if (learner == null) {
            throw new RuntimeException("El usuario no tiene un perfil de estudiante");
        }

        LearningSession session = learningSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Sesión no encontrada con ID: " + sessionId));

        System.out.println("[WAITLIST] Sesión encontrada: " + session.getTitle());

        if (! SessionStatus.SCHEDULED.equals(session.getStatus())) {
            throw new RuntimeException("No se puede unir a lista de espera de una sesión que no está programada");
        }

        List<Booking> existingBookings = bookingRepository.findByLearnerIdAndLearningSessionId(learner.getId(), sessionId);

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

        // Validar balance para sesiones Premium (solo validar, no cobrar aún)
        if (Boolean.TRUE.equals(session.getIsPremium())) {
            validateSkillCoinsBalance(learner, session);
        }

        long waitlistCount = bookingRepository.countByLearningSessionIdAndStatus(sessionId, BookingStatus.WAITING);

        System.out.println("[WAITLIST] Usuarios en lista de espera: " + waitlistCount);

        if (waitlistCount >= 20) {
            throw new RuntimeException("Lista de espera llena.Máximo 20 usuarios permitidos.");
        }

        Booking waitlistBooking = null;
        for (Booking booking : existingBookings) {
            if (BookingStatus.CANCELLED.equals(booking.getStatus())) {
                waitlistBooking = booking;
                System.out.println("[WAITLIST] Reutilizando booking CANCELLED existente");
                break;
            }
        }

        if (waitlistBooking == null) {
            waitlistBooking = new Booking();
            waitlistBooking.setLearningSession(session);
            waitlistBooking.setLearner(learner);
            System.out.println("[WAITLIST] Creando nuevo booking");
        }

        waitlistBooking.setType(BookingType.INDIVIDUAL);
        waitlistBooking.setStatus(BookingStatus.WAITING);
        waitlistBooking.setAttended(false);
        waitlistBooking.setAccessLink(null);

        Booking savedBooking = bookingRepository.save(waitlistBooking);

        System.out.println("[WAITLIST] Usuario agregado a lista de espera - Posición: " + (waitlistCount + 1));

        try {
            bookingEmailService.sendWaitlistConfirmationEmail(savedBooking, person);
            System.out.println("[WAITLIST] Email de confirmación enviado");
        } catch (Exception e) {
            System.err.println("[WAITLIST] Error al enviar email: " + e.getMessage());
        }

        return savedBooking;
    }

    /**
     * Procesa la lista de espera
     */
    @Transactional
    public void processWaitlist(Long sessionId) {

        System.out.println("[WAITLIST] Procesando lista de espera para sesión: " + sessionId);

        LearningSession session = learningSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Sesión no encontrada"));

        long confirmedBookings = bookingRepository.countConfirmedBookingsBySessionId(sessionId);
        int availableSpots = session.getMaxCapacity() - (int) confirmedBookings;

        System.out.println("[WAITLIST] Cupos disponibles: " + availableSpots);

        if (availableSpots <= 0) {
            System.out.println("[WAITLIST] No hay cupos disponibles");
            return;
        }

        List<Booking> waitlist = bookingRepository.findByLearningSessionIdAndStatusOrderByBookingDateAsc(sessionId, BookingStatus.WAITING);

        if (waitlist.isEmpty()) {
            System.out.println("[WAITLIST] No hay usuarios en lista de espera");
            return;
        }

        int spotsToFill = Math.min(availableSpots, waitlist.size());

        for (int i = 0; i < spotsToFill; i++) {
            Booking waitlistBooking = waitlist.get(i);
            Learner learner = waitlistBooking.getLearner();

            // Procesar pago para sesiones Premium al promover de lista de espera
            if (Boolean.TRUE.equals(session.getIsPremium())) {
                try {
                    processSkillCoinsPayment(learner, session);
                } catch (RuntimeException e) {
                    System.err.println("[WAITLIST] Usuario " + learner.getPerson().getEmail() +
                            " no tiene fondos suficientes: " + e.getMessage());
                    continue;
                }
            }

            waitlistBooking.setStatus(BookingStatus.CONFIRMED);
            waitlistBooking.setAccessLink(session.getVideoCallLink());
            bookingRepository.save(waitlistBooking);

            System.out.println("[WAITLIST] Usuario promovido de lista de espera a confirmado: " +
                    learner.getPerson().getEmail());

            try {
                Person person = learner.getPerson();
                bookingEmailService.sendSpotAvailableEmail(waitlistBooking, person);
                System.out.println("[WAITLIST] Email de cupo disponible enviado");
            } catch (Exception e) {
                System.err.println("[WAITLIST] Error al enviar email: " + e.getMessage());
            }
        }

        System.out.println("[WAITLIST] Procesamiento completado. " + spotsToFill + " usuarios promovidos.");
    }

    /**
     * Permite que un usuario salga de la lista de espera
     */
    @Transactional
    public void leaveWaitlist(Long bookingId, String userEmail) {

        System.out.println("[WAITLIST] Usuario saliendo de lista de espera - Booking ID: " + bookingId);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking no encontrado con ID: " + bookingId));

        Person person = personRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (!booking.getLearner().getId().equals(person.getLearner().getId())) {
            throw new RuntimeException("No tienes permiso para modificar este booking");
        }

        if (! BookingStatus.WAITING.equals(booking.getStatus())) {
            throw new RuntimeException("Solo puedes salir de una lista de espera si estás en estado WAITING");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        System.out.println("[WAITLIST] Usuario removido de lista de espera exitosamente");

        try {
            bookingEmailService.sendWaitlistExitConfirmationEmail(booking, person);
            System.out.println("[WAITLIST] Email de confirmación de salida enviado");
        } catch (Exception e) {
            System.err.println("[WAITLIST] Error al enviar email: " + e.getMessage());
        }
    }

    /**
     * Cancela un booking individual o grupal
     */
    @Transactional
    public Booking cancelBooking(Long bookingId, String userEmail) {

        System.out.println("[BOOKING_CANCEL] Iniciando cancelación de booking: " + bookingId);

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

    private Booking cancelIndividualBooking(Booking booking, Person person, LearningSession session) {

        System.out.println("[BOOKING_CANCEL] Cancelando booking individual: " + booking.getId());

        // Reembolsar SkillCoins si era sesión Premium
        if (Boolean.TRUE.equals(session.getIsPremium()) && BookingStatus.CONFIRMED.equals(booking.getStatus())) {
            refundSkillCoins(booking.getLearner(), session);
        }

        booking.setStatus(BookingStatus.CANCELLED);
        Booking cancelledBooking = bookingRepository.save(booking);

        System.out.println("[SUCCESS] Booking individual cancelado. Cupo liberado: 1");

        sendCancellationNotifications(cancelledBooking, person, session, false, 1);

        System.out.println("[WAITLIST] Procesando lista de espera después de cancelación...");
        try {
            processWaitlist(session.getId());
        } catch (Exception e) {
            System.err.println("[WAITLIST] Error al procesar lista de espera: " + e.getMessage());
        }

        return cancelledBooking;
    }

    private Booking cancelGroupBooking(Booking booking, Person person, LearningSession session) {

        System.out.println("[BOOKING_CANCEL] Cancelando booking grupal");

        LearningCommunity community = booking.getCommunity();
        if (community == null) {
            throw new RuntimeException("No se puede determinar la comunidad del booking grupal");
        }

        List<Booking> groupBookings = bookingRepository.findByLearningSessionIdAndCommunityId(session.getId(), community.getId());

        int cancelledCount = 0;
        for (Booking groupBooking : groupBookings) {
            if (! BookingStatus.CANCELLED.equals(groupBooking.getStatus())) {
                // Reembolsar SkillCoins si era sesión Premium
                if (Boolean.TRUE.equals(session.getIsPremium()) && BookingStatus.CONFIRMED.equals(groupBooking.getStatus())) {
                    refundSkillCoins(groupBooking.getLearner(), session);
                }

                groupBooking.setStatus(BookingStatus.CANCELLED);
                bookingRepository.save(groupBooking);
                cancelledCount++;
            }
        }

        System.out.println("[SUCCESS] Booking grupal cancelado. Cupos liberados: " + cancelledCount);

        sendCancellationNotifications(booking, person, session, true, cancelledCount);

        System.out.println("[WAITLIST] Procesando lista de espera después de cancelación grupal...");
        try {
            processWaitlist(session.getId());
        } catch (Exception e) {
            System.err.println("[WAITLIST] Error al procesar lista de espera: " + e.getMessage());
        }

        return booking;
    }

    private void sendCancellationNotifications(Booking booking, Person learnerPerson,
                                               LearningSession session, boolean isGroup, int spotsFreed) {
        try {
            bookingEmailService.sendBookingCancellationEmail(booking, learnerPerson);
            System.out.println("[EMAIL] Confirmación de cancelación enviada a: " + learnerPerson.getEmail());

            Person instructorPerson = session.getInstructor().getPerson();
            bookingEmailService.sendInstructorNotificationEmail(session, instructorPerson, learnerPerson.getFullName(), isGroup, spotsFreed);
            System.out.println("[EMAIL] Notificación enviada al instructor: " + instructorPerson.getEmail());

        } catch (Exception e) {
            System.err.println("[ERROR] Error al enviar notificaciones: " + e.getMessage());
        }
    }
}