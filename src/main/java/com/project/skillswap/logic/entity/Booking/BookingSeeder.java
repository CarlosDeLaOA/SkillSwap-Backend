package com.project.skillswap.logic.entity.Booking;

import com.project.skillswap.logic.entity.LearningSession.LearningSession;
import com.project.skillswap.logic.entity.LearningSession.LearningSessionRepository;
import com.project.skillswap.logic.entity.Learner.Learner;
import com.project.skillswap.logic.entity.Learner.LearnerRepository;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Seeder component that creates initial bookings in the database
 */
@Order(7)
@Component
public class BookingSeeder implements ApplicationListener<ContextRefreshedEvent> {

    //#region Dependencies
    private final BookingRepository bookingRepository;
    private final LearningSessionRepository learningSessionRepository;
    private final LearnerRepository learnerRepository;
    //#endregion

    //#region Constructor
    /**
     * Creates a new BookingSeeder instance
     *
     * @param bookingRepository the booking repository
     * @param learningSessionRepository the learning session repository
     * @param learnerRepository the learner repository
     */
    public BookingSeeder(
            BookingRepository bookingRepository,
            LearningSessionRepository learningSessionRepository,
            LearnerRepository learnerRepository) {
        this.bookingRepository = bookingRepository;
        this.learningSessionRepository = learningSessionRepository;
        this.learnerRepository = learnerRepository;
    }
    //#endregion

    //#region Event Handling
    /**
     * Handles the application context refreshed event to seed initial data
     *
     * @param event the context refreshed event
     */
    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        this.seedBookings();
    }
    //#endregion

    //#region Seeding Logic
    /**
     * Seeds bookings into the database
     */
    private void seedBookings() {
        List<BookingData> bookingsToCreate = createBookingDataList();

        for (BookingData bookingData : bookingsToCreate) {
            Optional<LearningSession> session = learningSessionRepository.findById(bookingData.learningSessionId);
            Optional<Learner> learner = learnerRepository.findById(bookingData.learnerId);

            if (session.isEmpty() || learner.isEmpty()) {
                continue;
            }

            Optional<Booking> existingBooking = bookingRepository.findByLearningSessionAndLearner(
                    session.get(), learner.get()
            );

            if (existingBooking.isPresent()) {
                continue;
            }

            Booking booking = createBooking(bookingData, session.get(), learner.get());
            bookingRepository.save(booking);
        }

    }

    /**
     * Creates a Booking entity from BookingData
     *
     * @param data the booking data
     * @param session the learning session
     * @param learner the learner
     * @return the created Booking entity
     */
    private Booking createBooking(BookingData data, LearningSession session, Learner learner) {
        Booking booking = new Booking();
        booking.setLearningSession(session);
        booking.setLearner(learner);
        booking.setType(data.type);
        booking.setStatus(data.status);
        booking.setAccessLink(data.accessLink);
        booking.setAttended(data.attended);
        return booking;
    }

    /**
     * Creates the list of booking data to be seeded
     *
     * @return list of BookingData objects
     */
    private List<BookingData> createBookingDataList() {
        List<BookingData> bookings = new ArrayList<>();

        bookings.add(new BookingData(1L, 1L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED,
                "https://meet.google.com/maria-esp-001", true));
        bookings.add(new BookingData(2L, 2L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED,
                "https://meet.google.com/sophie-py-001", true));
        bookings.add(new BookingData(3L, 2L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED,
                "https://meet.google.com/carlos-ml-103", false));

        bookings.add(new BookingData(4L, 5L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED,
                "https://meet.google.com/yuki-cal-201", true));
        bookings.add(new BookingData(5L, 5L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED,
                "https://meet.google.com/lucas-guitar-001", true));

        bookings.add(new BookingData(6L, 1L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED,
                "https://meet.google.com/ahmed-sql-001", false));

        bookings.add(new BookingData(20L, 1L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED,
                "https://meet.google.com/carlos-ml-103", true));
        bookings.add(new BookingData(21L, 2L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED,
                "https://meet.google.com/carlos-dl-108", true));

        bookings.add(new BookingData(28L, 1L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED,
                "https://meet.google.com/yuki-cal-201", true));
        bookings.add(new BookingData(29L, 2L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED,
                "https://meet.google.com/yuki-ink-203", true));

        bookings.add(new BookingData(30L, 1L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED,
                "https://meet.google.com/maria-viajes-003", true));
        bookings.add(new BookingData(31L, 2L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED,
                "https://meet.google.com/sophie-js-003", true));
        bookings.add(new BookingData(32L, 5L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED,
                "https://meet.google.com/yuki-orig-205", true));
        bookings.add(new BookingData(33L, 2L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED,
                "https://meet.google.com/carlos-pandas-109", false));
        bookings.add(new BookingData(34L, 8L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED,
                "https://meet.google.com/lucas-electric-002", true));
        bookings.add(new BookingData(35L, 13L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED,
                "https://meet.google.com/ahmed-sql-opt-002", true));
        bookings.add(new BookingData(36L, 1L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED,
                "https://meet.google.com/elena-algebra-001", true));
        bookings.add(new BookingData(37L, 15L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED,
                "https://meet.google.com/li-pinyin-001", false));
        bookings.add(new BookingData(38L, 1L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED,
                "https://meet.google.com/clara-figma-001", true));
        bookings.add(new BookingData(39L, 20L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED,
                "https://meet.google.com/marco-yoga-001", true));

        bookings.add(new BookingData(40L, 5L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED,
                "https://meet.google.com/maria-negocios-004", true));
        bookings.add(new BookingData(41L, 8L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED,
                "https://meet.google.com/sophie-dom-004", false));
        bookings.add(new BookingData(42L, 2L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED,
                "https://meet.google.com/yuki-ike-206", true));
        bookings.add(new BookingData(43L, 13L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED,
                "https://meet.google.com/carlos-seaborn-110", false));
        bookings.add(new BookingData(44L, 15L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED,
                "https://meet.google.com/lucas-finger-003", true));
        bookings.add(new BookingData(45L, 17L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED,
                "https://meet.google.com/ahmed-bi-003", false));
        bookings.add(new BookingData(46L, 20L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED,
                "https://meet.google.com/elena-calculo-002", true));
        bookings.add(new BookingData(47L, 2L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED,
                "https://meet.google.com/li-conversacion-002", true));
        bookings.add(new BookingData(48L, 5L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED,
                "https://meet.google.com/clara-branding-002", false));
        bookings.add(new BookingData(49L, 8L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED,
                "https://meet.google.com/marco-vinyasa-002", true));
        bookings.add(new BookingData(50L, 2L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED,
                "https://meet.google.com/ana-foto-movil-050", true));
        bookings.add(new BookingData(51L, 5L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED,
                "https://meet.google.com/david-micro-052", true));
        bookings.add(new BookingData(52L, 8L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED,
                "https://meet.google.com/isa-pasta-054", true));
        bookings.add(new BookingData(53L, 13L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED,
                "https://meet.google.com/miguel-seo-056", false));
        bookings.add(new BookingData(54L, 15L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED,
                "https://meet.google.com/natalie-viajes-058", true));
        bookings.add(new BookingData(55L, 17L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED,
                "https://meet.google.com/sara-gtd-060", true));
        bookings.add(new BookingData(56L, 20L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED,
                "https://meet.google.com/thomas-git-062", false));
        bookings.add(new BookingData(57L, 2L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED,
                "https://meet.google.com/oliver-auto-064", true));
        bookings.add(new BookingData(58L, 5L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED,
                "https://meet.google.com/clara-tipo-066", true));
        bookings.add(new BookingData(59L, 8L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED,
                "https://meet.google.com/marco-restaurativo-068", true));
        bookings.add(new BookingData(60L, 13L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED,
                "https://meet.google.com/ana-nocturna-051", false));
        bookings.add(new BookingData(61L, 15L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED,
                "https://meet.google.com/david-clean-053", true));
        bookings.add(new BookingData(62L, 17L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED,
                "https://meet.google.com/isa-pizza-055", true));
        bookings.add(new BookingData(63L, 20L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED,
                "https://meet.google.com/miguel-ads-057", false));
        bookings.add(new BookingData(64L, 2L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED,
                "https://meet.google.com/natalie-comercial-059", true));
        bookings.add(new BookingData(65L, 5L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED,
                "https://meet.google.com/sara-oratoria-061", true));
        bookings.add(new BookingData(66L, 8L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED,
                "https://meet.google.com/thomas-actions-063", false));
        bookings.add(new BookingData(67L, 13L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED,
                "https://meet.google.com/oliver-finanzas-065", true));
        bookings.add(new BookingData(68L, 15L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED,
                "https://meet.google.com/clara-motion-067", true));
        bookings.add(new BookingData(69L, 17L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED,
                "https://meet.google.com/marco-oficina-069", true));
        bookings.add(new BookingData(70L, 1L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED,
                "https://meet.google.com/carlos-datascience-201", false));

        bookings.add(new BookingData(71L, 1L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED,
                "https://meet.google.com/carlos-devops-202", false));

        bookings.add(new BookingData(72L, 1L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED,
                "https://meet.google.com/carlos-mlops-203", false));

        bookings.add(new BookingData(73L, 1L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED,
                "https://meet.google.com/carlos-jsasync-204", false));

        bookings.add(new BookingData(74L, 1L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED,
                "https://meet.google.com/carlos-reactts-205", false));

        bookings.add(new BookingData(75L, 1L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED,
                "https://meet.google.com/carlos-pyinter-101", true));

        bookings.add(new BookingData(76L, 1L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED,
                "https://meet.google.com/carlos-sqlanalysis-102", true));

        bookings.add(new BookingData(77L, 1L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED,
                "https://meet.google.com/carlos-gitflow-103", true));

        bookings.add(new BookingData(78L, 1L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED,
                "https://meet.google.com/carlos-nodejs-104", true));

        bookings.add(new BookingData(1L, 1L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED,
                "https://meet.google.com/maria-esp-001", true));

        bookings.add(new BookingData(4L, 1L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED,
                "https://meet.google.com/yuki-cal-201", true));

        bookings.add(new BookingData(50L, 1L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED,
                "https://meet.google.com/ana-foto-movil-050", true));

        bookings.add(new BookingData(52L, 1L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED,
                "https://meet.google.com/isa-pasta-054", true));

        bookings.add(new BookingData(55L, 1L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED,
                "https://meet.google.com/sara-gtd-060", true));

        bookings.add(new BookingData(39L, 1L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED,
                "https://meet.google.com/marco-yoga-001", true));
        return bookings;
    }
    //#endregion

    //#region Inner Class
    /**
     * Data class holding information for creating bookings
     */
    private static class BookingData {
        Long learningSessionId;
        Long learnerId;
        BookingType type;
        BookingStatus status;
        String accessLink;
        Boolean attended;

        BookingData(Long learningSessionId, Long learnerId, BookingType type,
                    BookingStatus status, String accessLink, Boolean attended) {
            this.learningSessionId = learningSessionId;
            this.learnerId = learnerId;
            this.type = type;
            this.status = status;
            this.accessLink = accessLink;
            this.attended = attended;
        }
    }
    //#endregion
}