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
            Optional<Learner> learner = learnerRepository.findById(bookingData.learnerId.intValue());

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

        bookings.add(new BookingData(
                1L, 1L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED,
                "https://meet.google.com/abc-defg-hij", false
        ));

        bookings.add(new BookingData(
                2L, 1L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED,
                "https://meet.google.com/klm-nopq-rst", false
        ));

        bookings.add(new BookingData(
                3L, 5L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED,
                "https://meet.google.com/uvw-xyza-bcd", false
        ));

        bookings.add(new BookingData(
                4L, 2L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED,
                "https://meet.google.com/efg-hijk-lmn", false
        ));

        bookings.add(new BookingData(
                5L, 1L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED,
                "https://meet.google.com/opq-rstu-vwx", true
        ));

        bookings.add(new BookingData(
                6L, 5L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED,
                "https://meet.google.com/yza-bcde-fgh", true
        ));

        bookings.add(new BookingData(
                7L, 2L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED,
                "https://meet.google.com/ijk-lmno-pqr", true
        ));

        bookings.add(new BookingData(
                8L, 8L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED,
                "https://meet.google.com/stu-vwxy-zab", false
        ));

        bookings.add(new BookingData(
                9L, 1L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED,
                "https://meet.google.com/cde-fghi-jkl", false
        ));

        bookings.add(new BookingData(
                10L, 2L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED,
                "https://meet.google.com/mno-pqrs-tuv", false
        ));

      bookings.add(new BookingData(
            20L, 1L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED,  // ← CAMBIO AQUÍ: 1L en vez de 2L
            "https://meet.google.com/john-carlos-101", true
    ));

    bookings.add(new BookingData(
            21L, 1L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED,
            "https://meet.google.com/john-carlos-102", true
    ));

    bookings.add(new BookingData(
            22L, 1L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED,
            "https://meet.google.com/john-carlos-103", true
    ));

    bookings.add(new BookingData(
            23L, 1L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED,
            "https://meet.google.com/john-carlos-104", true
    ));

    bookings.add(new BookingData(
            24L, 1L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED,
            "https://meet.google.com/john-carlos-105", true
    ));

    bookings.add(new BookingData(
            25L, 1L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED,
            "https://meet.google.com/john-carlos-106", true
    ));

    bookings.add(new BookingData(
            26L, 1L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED,
            "https://meet.google.com/john-carlos-107", true
    ));

    bookings.add(new BookingData(
            27L, 1L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED,
            "https://meet.google.com/john-carlos-108", true
    ));

    // Sesiones de Yuki Tanaka
    bookings.add(new BookingData(
            28L, 1L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED,
            "https://meet.google.com/john-yuki-201", true
    ));

    bookings.add(new BookingData(
            29L, 1L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED,
            "https://meet.google.com/john-yuki-202", true
    ));

    bookings.add(new BookingData(
            30L, 1L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED,
            "https://meet.google.com/john-yuki-203", true
    ));

    bookings.add(new BookingData(
            31L, 1L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED,
            "https://meet.google.com/john-yuki-204", true
    ));

    bookings.add(new BookingData(
            32L, 1L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED,
            "https://meet.google.com/john-yuki-205", true
    ));

    bookings.add(new BookingData(
            33L, 1L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED,
            "https://meet.google.com/john-yuki-206", true
    ));

    bookings.add(new BookingData(
            34L, 1L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED,
            "https://meet.google.com/john-yuki-207", true
    ));

    bookings.add(new BookingData(
            35L, 1L, BookingType.INDIVIDUAL, BookingStatus.CONFIRMED,
            "https://meet.google.com/john-yuki-208", true
    ));


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