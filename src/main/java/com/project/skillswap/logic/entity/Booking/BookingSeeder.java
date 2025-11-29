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
        booking.setAccessLink(session.getVideoCallLink());
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

        bookings.add(new BookingData(1L, 1L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED, true));
        bookings.add(new BookingData(2L, 2L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED, true));
        bookings.add(new BookingData(3L, 2L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED, false));

        bookings.add(new BookingData(4L, 5L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED, true));
        bookings.add(new BookingData(5L, 5L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED, true));

        bookings.add(new BookingData(6L, 1L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED, false));

        bookings.add(new BookingData(20L, 1L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED, true));
        bookings.add(new BookingData(21L, 2L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED, true));

        bookings.add(new BookingData(28L, 1L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED, true));
        bookings.add(new BookingData(29L, 2L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED, true));

        bookings.add(new BookingData(30L, 1L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED, true));
        bookings.add(new BookingData(31L, 2L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED, true));
        bookings.add(new BookingData(32L, 5L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED, true));
        bookings.add(new BookingData(33L, 2L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED, false));
        bookings.add(new BookingData(34L, 8L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED, true));
        bookings.add(new BookingData(35L, 13L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED, true));
        bookings.add(new BookingData(36L, 1L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED, true));
        bookings.add(new BookingData(37L, 15L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED, false));
        bookings.add(new BookingData(38L, 1L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED, true));
        bookings.add(new BookingData(39L, 20L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED, true));

        bookings.add(new BookingData(40L, 5L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED, true));
        bookings.add(new BookingData(41L, 8L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED, false));
        bookings.add(new BookingData(42L, 2L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED, true));
        bookings.add(new BookingData(43L, 13L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED, false));
        bookings.add(new BookingData(44L, 15L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED, true));
        bookings.add(new BookingData(45L, 17L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED, false));
        bookings.add(new BookingData(46L, 20L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED, true));
        bookings.add(new BookingData(47L, 2L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED, true));
        bookings.add(new BookingData(48L, 5L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED, false));
        bookings.add(new BookingData(49L, 8L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED, true));
        bookings.add(new BookingData(50L, 2L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED, true));
        bookings.add(new BookingData(51L, 5L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED, true));
        bookings.add(new BookingData(52L, 8L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED, true));
        bookings.add(new BookingData(53L, 13L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED, false));
        bookings.add(new BookingData(54L, 15L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED, true));
        bookings.add(new BookingData(55L, 17L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED, true));
        bookings.add(new BookingData(56L, 20L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED, false));
        bookings.add(new BookingData(57L, 2L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED, true));
        bookings.add(new BookingData(58L, 5L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED, true));
        bookings.add(new BookingData(59L, 8L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED, true));
        bookings.add(new BookingData(60L, 13L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED, false));
        bookings.add(new BookingData(61L, 15L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED, true));
        bookings.add(new BookingData(62L, 17L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED, true));
        bookings.add(new BookingData(63L, 20L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED, false));
        bookings.add(new BookingData(64L, 2L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED, true));
        bookings.add(new BookingData(65L, 5L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED, true));
        bookings.add(new BookingData(66L, 8L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED, false));
        bookings.add(new BookingData(67L, 13L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED, true));
        bookings.add(new BookingData(68L, 15L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED, true));
        bookings.add(new BookingData(69L, 17L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED, true));
        bookings.add(new BookingData(70L, 1L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED, false));

        bookings.add(new BookingData(71L, 1L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED, false));

        bookings.add(new BookingData(72L, 1L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED, false));

        bookings.add(new BookingData(73L, 1L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED, false));

        bookings.add(new BookingData(74L, 1L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED, false));

        bookings.add(new BookingData(75L, 1L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED, true));

        bookings.add(new BookingData(76L, 1L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED, true));

        bookings.add(new BookingData(77L, 1L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED, true));

        bookings.add(new BookingData(78L, 1L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED, true));

        bookings.add(new BookingData(1L, 1L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED, true));

        bookings.add(new BookingData(4L, 1L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED, true));

        bookings.add(new BookingData(50L, 1L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED, true));

        bookings.add(new BookingData(52L, 1L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED, true));

        bookings.add(new BookingData(55L, 1L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED, true));

        bookings.add(new BookingData(39L, 1L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED, true));
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
        Boolean attended;

        BookingData(Long learningSessionId, Long learnerId, BookingType type,
                    BookingStatus status, Boolean attended) {
            this.learningSessionId = learningSessionId;
            this.learnerId = learnerId;
            this.type = type;
            this.status = status;
            this.attended = attended;
        }
    }
    //#endregion
}