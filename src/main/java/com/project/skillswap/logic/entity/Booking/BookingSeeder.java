package com.project.skillswap.logic.entity.Booking;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import com.project.skillswap.logic.entity.LearningSession.LearningSession;
import com.project.skillswap.logic.entity.LearningSession.LearningSessionRepository;
import com.project.skillswap.logic.entity.LearningSession.SessionStatus;
import com.project.skillswap.logic.entity.Learner.Learner;
import com.project.skillswap.logic.entity.Learner.LearnerRepository;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.*;

@Order(6)
@Component
public class BookingSeeder implements ApplicationListener<ContextRefreshedEvent> {
    private static final Logger logger = LoggerFactory.getLogger(BookingSeeder.class);

    private final BookingRepository bookingRepository;
    private final LearningSessionRepository learningSessionRepository;
    private final LearnerRepository learnerRepository;
    private final Random random = new Random();

    public BookingSeeder(BookingRepository bookingRepository,
                         LearningSessionRepository learningSessionRepository,
                         LearnerRepository learnerRepository) {
        this.bookingRepository = bookingRepository;
        this.learningSessionRepository = learningSessionRepository;
        this.learnerRepository = learnerRepository;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (bookingRepository.count() > 0) {
            logger.info("BookingSeeder: Ya existen bookings, omitiendo seed");
            return;
        }
        this.seedBookings();
    }

    private void seedBookings() {
        List<Learner> learners = learnerRepository.findAll();
        List<LearningSession> sessions = learningSessionRepository.findAll();

        if (learners.isEmpty() || sessions.isEmpty()) {
            logger.warn("No hay learners o sesiones para crear bookings");
            return;
        }

        // Separar sesiones pasadas y futuras
        List<LearningSession> pastSessions = new ArrayList<>();
        List<LearningSession> futureSessions = new ArrayList<>();

        for (LearningSession session : sessions) {
            if (session.getStatus() == SessionStatus.FINISHED) {
                pastSessions.add(session);
            } else if (session.getStatus() == SessionStatus.SCHEDULED) {
                futureSessions.add(session);
            }
        }

        // Para cada learner, crear bookings
        for (Learner learner : learners) {
            // Bookings en sesiones pasadas (attended = true)
            int completedSessions = learner.getCompletedSessions();
            List<LearningSession> selectedPastSessions = selectRandomSessions(pastSessions, completedSessions);

            for (LearningSession session : selectedPastSessions) {
                Booking booking = createBooking(learner, session, true);
                bookingRepository.save(booking);
            }

            // Bookings en sesiones futuras (mínimo 10)
            int futureBookingsCount = 10 + random.nextInt(5); // 10-15 bookings futuras
            List<LearningSession> selectedFutureSessions = selectRandomSessions(futureSessions, futureBookingsCount);

            for (LearningSession session : selectedFutureSessions) {
                Booking booking = createBooking(learner, session, false);
                bookingRepository.save(booking);
            }
        }

        logger.info("BookingSeeder: Bookings creadas exitosamente");
    }

    private Booking createBooking(Learner learner, LearningSession session, boolean isPast) {
        Booking booking = new Booking();

        booking.setLearner(learner);
        booking.setLearningSession(session);
        booking.setType(BookingType.INDIVIDUAL);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setAccessLink(session.getVideoCallLink());

        if (isPast) {
            // Sesión pasada - el learner asistió
            booking.setAttended(true);

            // Entry y exit time
            Calendar entryCal = Calendar.getInstance();
            entryCal.setTime(session.getScheduledDatetime());
            entryCal.add(Calendar.MINUTE, -random.nextInt(5)); // Llegó 0-5 min antes
            booking.setEntryTime(entryCal.getTime());

            Calendar exitCal = Calendar.getInstance();
            exitCal.setTime(session.getScheduledDatetime());
            exitCal.add(Calendar.MINUTE, session.getDurationMinutes() + random.nextInt(10)); // Se quedó un poco más
            booking.setExitTime(exitCal.getTime());
        } else {
            // Sesión futura - aún no asiste
            booking.setAttended(false);
        }

        // Fecha de booking (antes de la sesión)
        Calendar bookingCal = Calendar.getInstance();
        bookingCal.setTime(session.getScheduledDatetime());
        bookingCal.add(Calendar.DAY_OF_MONTH, -random.nextInt(14) - 1); // Reservó 1-14 días antes
        booking.setBookingDate(bookingCal.getTime());

        return booking;
    }

    private List<LearningSession> selectRandomSessions(List<LearningSession> sessions, int count) {
        if (sessions.isEmpty()) return new ArrayList<>();

        List<LearningSession> selected = new ArrayList<>();
        List<LearningSession> available = new ArrayList<>(sessions);

        for (int i = 0; i < count && !available.isEmpty(); i++) {
            int index = random.nextInt(available.size());
            selected.add(available.get(index));
            available.remove(index); // Evitar duplicados
        }

        return selected;
    }
}