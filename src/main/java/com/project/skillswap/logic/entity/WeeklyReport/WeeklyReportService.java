package com.project.skillswap.logic.entity.WeeklyReport;

import com.project.skillswap.logic.entity.Attendancerecord.AttendanceRecord;
import com.project.skillswap.logic.entity.Attendancerecord.AttendanceRecordRepository;
import com.project.skillswap.logic.entity.Booking.Booking;
import com.project.skillswap.logic.entity.Booking.BookingRepository;
import com.project.skillswap.logic.entity.Credential.Credential;
import com.project.skillswap.logic.entity.Credential.CredentialRepository;
import com.project.skillswap.logic.entity.Feedback.Feedback;
import com.project.skillswap.logic.entity.Feedback.FeedbackRepository;
import com.project.skillswap.logic.entity.Instructor.Instructor;
import com.project.skillswap.logic.entity.Learner.Learner;
import com.project.skillswap.logic.entity.LearningSession.LearningSession;
import com.project.skillswap.logic.entity.LearningSession.LearningSessionRepository;
import com.project.skillswap.logic.entity.Person.Person;
import com.project.skillswap.logic.entity.Person.PersonRepository;
import com.project.skillswap.logic.entity.Transaction.Transaction;
import com.project.skillswap.logic.entity.Transaction.TransactionRepository;
import com.project.skillswap.logic.entity.Transaction.TransactionType;
import com.project.skillswap.logic.entity.UserSkill.UserSkill;
import com.project.skillswap.logic.entity.UserSkill.UserSkillRepository;
import jakarta.mail.MessagingException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio que gestiona la generación y envío de reportes semanales.
 */
@Service
public class WeeklyReportService {

    //#region Dependencies
    private final PersonRepository personRepository;
    private final WeeklyReportRepository weeklyReportRepository;
    private final CredentialRepository credentialRepository;
    private final BookingRepository bookingRepository;
    private final TransactionRepository transactionRepository;
    private final FeedbackRepository feedbackRepository;
    private final LearningSessionRepository learningSessionRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final UserSkillRepository userSkillRepository;
    private final WeeklyReportEmailService emailService;

    public WeeklyReportService(PersonRepository personRepository,
                               WeeklyReportRepository weeklyReportRepository,
                               CredentialRepository credentialRepository,
                               BookingRepository bookingRepository,
                               TransactionRepository transactionRepository,
                               FeedbackRepository feedbackRepository,
                               LearningSessionRepository learningSessionRepository,
                               AttendanceRecordRepository attendanceRecordRepository,
                               UserSkillRepository userSkillRepository,
                               WeeklyReportEmailService emailService) {
        this.personRepository = personRepository;
        this.weeklyReportRepository = weeklyReportRepository;
        this.credentialRepository = credentialRepository;
        this.bookingRepository = bookingRepository;
        this.transactionRepository = transactionRepository;
        this.feedbackRepository = feedbackRepository;
        this.learningSessionRepository = learningSessionRepository;
        this.attendanceRecordRepository = attendanceRecordRepository;
        this.userSkillRepository = userSkillRepository;
        this.emailService = emailService;
    }
    //#endregion

    //#region Public Methods
    /**
     * Genera y envía reportes semanales a todos los usuarios activos.
     */
    @Transactional
    public void generateAndSendWeeklyReports() {
        LocalDate[] weekDates = getLastWeekDates();
        Date weekStartDate = Date.from(weekDates[0].atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date weekEndDate = Date.from(weekDates[1].atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant());

        List<Person> activePersons = personRepository.findAll().stream()
                .filter(Person::getActive)
                .filter(Person::getEmailVerified)
                .collect(Collectors.toList());

        for (Person person : activePersons) {
            try {
                generateAndSendPersonReport(person, weekStartDate, weekEndDate);
            } catch (Exception e) {
                System.err.println("Error generando reporte para " + person.getEmail() + ": " + e.getMessage());
            }
        }
    }

    /**
     * Limpia reportes antiguos de la base de datos.
     *
     * @param monthsToKeep cantidad de meses a mantener
     */
    @Transactional
    public void cleanOldReports(int monthsToKeep) {
        LocalDate cutoffDate = LocalDate.now().minusMonths(monthsToKeep);
        Date cutoffDateAsDate = Date.from(cutoffDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        weeklyReportRepository.deleteByReportDateBefore(cutoffDateAsDate);
    }
    //#endregion

    //#region Private Methods
    /**
     * Genera y envía el reporte para una persona específica.
     *
     * @param person persona
     * @param weekStartDate fecha de inicio de semana
     * @param weekEndDate fecha de fin de semana
     */
    private void generateAndSendPersonReport(Person person, Date weekStartDate, Date weekEndDate) {
        String role = person.getRole();

        if ("LEARNER".equals(role) || "BOTH".equals(role)) {
            generateLearnerReport(person, weekStartDate, weekEndDate);
        }

        if ("INSTRUCTOR".equals(role) || "BOTH".equals(role)) {
            generateInstructorReport(person, weekStartDate, weekEndDate);
        }
    }

    /**
     * Genera el reporte para un aprendiz.
     *
     * @param person persona
     * @param weekStartDate fecha de inicio de semana
     * @param weekEndDate fecha de fin de semana
     */
    private void generateLearnerReport(Person person, Date weekStartDate, Date weekEndDate) {
        Learner learner = person.getLearner();
        if (learner == null) return;

        int credentialsObtained = countCredentialsInWeek(learner.getId(), weekStartDate, weekEndDate);
        int sessionsAttended = countAttendedSessionsInWeek(learner.getId(), weekStartDate, weekEndDate);
        int skillcoinsInvested = calculateSkillcoinsInvestedInWeek(person.getId(), weekStartDate, weekEndDate);
        List<String> recommendations = generateSessionRecommendations(person);

        WeeklyReport report = new WeeklyReport();
        report.setPerson(person);
        report.setWeekStartDate(weekStartDate);
        report.setWeekEndDate(weekEndDate);
        report.setUserType("LEARNER");
        report.setCredentialsObtained(credentialsObtained);
        report.setSessionsAttended(sessionsAttended);
        report.setSkillcoinsInvested(skillcoinsInvested);

        try {
            WeeklyReportEmailService.LearnerReportData data = new WeeklyReportEmailService.LearnerReportData(
                    credentialsObtained,
                    sessionsAttended,
                    skillcoinsInvested,
                    recommendations
            );

            emailService.sendLearnerWeeklyReport(person.getEmail(), person.getFullName(), data);
            report.setEmailSent(true);
        } catch (MessagingException e) {
            System.err.println("Error enviando correo a " + person.getEmail() + ": " + e.getMessage());
            report.setEmailSent(false);
        }

        weeklyReportRepository.save(report);
    }

    /**
     * Genera el reporte para un instructor.
     *
     * @param person persona
     * @param weekStartDate fecha de inicio de semana
     * @param weekEndDate fecha de fin de semana
     */
    private void generateInstructorReport(Person person, Date weekStartDate, Date weekEndDate) {
        Instructor instructor = person.getInstructor();
        if (instructor == null) return;

        int sessionsTaught = countSessionsTaughtInWeek(instructor.getId(), weekStartDate, weekEndDate);
        int totalHoursTaught = calculateTotalHoursTaughtInWeek(instructor.getId(), weekStartDate, weekEndDate);
        int skillcoinsEarned = calculateSkillcoinsEarnedInWeek(person.getId(), weekStartDate, weekEndDate);
        double averageRating = calculateAverageRatingInWeek(instructor.getId(), weekStartDate, weekEndDate);
        int totalReviews = countReviewsInWeek(instructor.getId(), weekStartDate, weekEndDate);

        WeeklyReport report = new WeeklyReport();
        report.setPerson(person);
        report.setWeekStartDate(weekStartDate);
        report.setWeekEndDate(weekEndDate);
        report.setUserType("INSTRUCTOR");
        report.setSessionsTaught(sessionsTaught);
        report.setTotalHoursTaught(totalHoursTaught);
        report.setSkillcoinsEarned(skillcoinsEarned);
        report.setAverageRatingWeek(averageRating);
        report.setTotalReviewsReceived(totalReviews);

        try {
            WeeklyReportEmailService.InstructorReportData data = new WeeklyReportEmailService.InstructorReportData(
                    sessionsTaught,
                    totalHoursTaught,
                    skillcoinsEarned,
                    averageRating,
                    totalReviews
            );

            emailService.sendInstructorWeeklyReport(person.getEmail(), person.getFullName(), data);
            report.setEmailSent(true);
        } catch (MessagingException e) {
            System.err.println("Error enviando correo a " + person.getEmail() + ": " + e.getMessage());
            report.setEmailSent(false);
        }

        weeklyReportRepository.save(report);
    }

    /**
     * Cuenta las credenciales obtenidas por un learner en la semana.
     *
     * @param learnerId ID del learner
     * @param startDate fecha de inicio
     * @param endDate fecha de fin
     * @return cantidad de credenciales
     */
    private int countCredentialsInWeek(Long learnerId, Date startDate, Date endDate) {
        return (int) credentialRepository.findAll().stream()
                .filter(c -> c.getLearner() != null && c.getLearner().getId().equals(learnerId))
                .filter(c -> c.getObtainedDate() != null)
                .filter(c -> !c.getObtainedDate().before(startDate) && !c.getObtainedDate().after(endDate))
                .count();
    }

    /**
     * Cuenta las sesiones atendidas por un learner en la semana.
     *
     * @param learnerId ID del learner
     * @param startDate fecha de inicio
     * @param endDate fecha de fin
     * @return cantidad de sesiones
     */
    private int countAttendedSessionsInWeek(Long learnerId, Date startDate, Date endDate) {
        return (int) bookingRepository.findByLearnerId(learnerId).stream()
                .filter(Booking::getAttended)
                .filter(b -> b.getEntryTime() != null)
                .filter(b -> !b.getEntryTime().before(startDate) && !b.getEntryTime().after(endDate))
                .count();
    }

    /**
     * Calcula los skillcoins invertidos en la semana.
     *
     * @param personId ID de la persona
     * @param startDate fecha de inicio
     * @param endDate fecha de fin
     * @return cantidad de skillcoins
     */
    private int calculateSkillcoinsInvestedInWeek(Long personId, Date startDate, Date endDate) {
        return transactionRepository.findAll().stream()
                .filter(t -> t.getPerson() != null && t.getPerson().getId().equals(personId))
                .filter(t -> t.getType() == TransactionType.SESSION_PAYMENT || t.getType() == TransactionType.PURCHASE)
                .filter(t -> t.getTransactionDate() != null)
                .filter(t -> !t.getTransactionDate().before(startDate) && !t.getTransactionDate().after(endDate))
                .map(Transaction::getSkillcoinsAmount)
                .filter(Objects::nonNull)
                .map(BigDecimal::intValue)
                .reduce(0, Integer::sum);
    }

    /**
     * Genera recomendaciones de sesiones basadas en idioma y habilidades.
     *
     * @param person persona
     * @return lista de recomendaciones
     */

    private List<String> generateSessionRecommendations(Person person) {
        List<UserSkill> userSkills = userSkillRepository.findActiveUserSkillsByPersonId(person.getId());
        String preferredLanguage = person.getPreferredLanguage();

        if (userSkills.isEmpty()) {
            return Collections.singletonList("Completa tu perfil seleccionando habilidades de interés");
        }

        List<Long> skillIds = userSkills.stream()
                .map(us -> us.getSkill().getId())
                .collect(Collectors.toList());

        List<LearningSession> upcomingSessions = learningSessionRepository.findAll().stream()
                .filter(ls -> ls.getScheduledDatetime() != null) // FIX
                .filter(ls -> ls.getScheduledDatetime().after(new Date())) // FIX
                .filter(ls -> ls.getSkill() != null && skillIds.contains(ls.getSkill().getId()))
                .filter(ls -> ls.getLanguage() == null || ls.getLanguage().equalsIgnoreCase(preferredLanguage))
                .limit(5)
                .collect(Collectors.toList());

        return upcomingSessions.stream()
                .map(ls -> ls.getTitle() + " - " + ls.getSkill().getName())
                .collect(Collectors.toList());
    }

    private int countSessionsTaughtInWeek(Long instructorId, Date startDate, Date endDate) {
        return (int) learningSessionRepository.findAll().stream()
                .filter(ls -> ls.getInstructor() != null && ls.getInstructor().getId().equals(instructorId))
                .filter(ls -> ls.getScheduledDatetime() != null) // FIX
                .filter(ls -> !ls.getScheduledDatetime().before(startDate) && !ls.getScheduledDatetime().after(endDate)) // FIX
                .filter(ls -> attendanceRecordRepository.findByLearningSession(ls).isPresent())
                .count();
    }

    private int calculateTotalHoursTaughtInWeek(Long instructorId, Date startDate, Date endDate) {
        return learningSessionRepository.findAll().stream()
                .filter(ls -> ls.getInstructor() != null && ls.getInstructor().getId().equals(instructorId))
                .filter(ls -> ls.getScheduledDatetime() != null) // FIX
                .filter(ls -> !ls.getScheduledDatetime().before(startDate) && !ls.getScheduledDatetime().after(endDate)) // FIX
                .map(ls -> attendanceRecordRepository.findByLearningSession(ls).orElse(null))
                .filter(Objects::nonNull)
                .map(AttendanceRecord::getActualDurationMinutes)
                .filter(Objects::nonNull)
                .reduce(0, Integer::sum) / 60;
    }

    private int calculateSkillcoinsEarnedInWeek(Long personId, Date startDate, Date endDate) {
        return transactionRepository.findAll().stream()
                .filter(t -> t.getPerson() != null && t.getPerson().getId().equals(personId))
                .filter(t -> t.getType() == TransactionType.SESSION_PAYMENT) // FIX: antes SESSION_EARNING
                .filter(t -> t.getTransactionDate() != null)
                .filter(t -> !t.getTransactionDate().before(startDate) && !t.getTransactionDate().after(endDate))
                .map(Transaction::getSkillcoinsAmount)
                .filter(Objects::nonNull)
                .map(BigDecimal::intValue)
                .reduce(0, Integer::sum);
    }

    private double calculateAverageRatingInWeek(Long instructorId, Date startDate, Date endDate) {
        List<Integer> ratings = learningSessionRepository.findAll().stream()
                .filter(ls -> ls.getInstructor() != null && ls.getInstructor().getId().equals(instructorId))
                .filter(ls -> ls.getScheduledDatetime() != null) // FIX
                .filter(ls -> !ls.getScheduledDatetime().before(startDate) && !ls.getScheduledDatetime().after(endDate)) // FIX
                .flatMap(ls -> feedbackRepository.findAll().stream()
                        .filter(f -> f.getLearningSession().equals(ls))
                        .filter(f -> f.getRating() != null))
                .map(Feedback::getRating)
                .collect(Collectors.toList());

        if (ratings.isEmpty()) {
            return 0.0;
        }

        return ratings.stream()
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0.0);
    }

    private int countReviewsInWeek(Long instructorId, Date startDate, Date endDate) {
        return (int) learningSessionRepository.findAll().stream()
                .filter(ls -> ls.getInstructor() != null && ls.getInstructor().getId().equals(instructorId))
                .filter(ls -> ls.getScheduledDatetime() != null) // FIX
                .filter(ls -> !ls.getScheduledDatetime().before(startDate) && !ls.getScheduledDatetime().after(endDate)) // FIX
                .flatMap(ls -> feedbackRepository.findAll().stream()
                        .filter(f -> f.getLearningSession().equals(ls))
                        .filter(f -> f.getCreationDate() != null)
                        .filter(f -> !f.getCreationDate().before(startDate) && !f.getCreationDate().after(endDate)))
                .count();
    }

    /**
     * Obtiene las fechas de inicio y fin de la semana pasada.
     *
     * @return array con fecha de inicio y fin
     */
    private LocalDate[] getLastWeekDates() {
        LocalDate today = LocalDate.now();
        LocalDate lastSunday = today.with(DayOfWeek.SUNDAY).minusWeeks(1);
        LocalDate lastMonday = lastSunday.minusDays(6);

        return new LocalDate[]{lastMonday, lastSunday};
    }
    //#endregion
}