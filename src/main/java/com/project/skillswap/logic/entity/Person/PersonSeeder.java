package com.project.skillswap.logic.entity.Person;

import com.project.skillswap.logic.entity.Instructor.Instructor;
import com.project.skillswap.logic.entity.Instructor.InstructorRepository;
import com.project.skillswap.logic.entity.Learner.Learner;
import com.project.skillswap.logic.entity.Learner.LearnerRepository;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Seeder component that creates initial test users in the database.
 */
@Order(1)
@Component
public class PersonSeeder implements ApplicationListener<ContextRefreshedEvent> {

    //#region Dependencies
    private final PersonRepository personRepository;
    private final InstructorRepository instructorRepository;
    private final LearnerRepository learnerRepository;
    private final PasswordEncoder passwordEncoder;
    //#endregion

    //#region Constructor
    /**
     * Creates a new PersonSeeder instance.
     *
     * @param personRepository the person repository
     * @param instructorRepository the instructor repository
     * @param learnerRepository the learner repository
     * @param passwordEncoder the password encoder
     */
    public PersonSeeder(
            PersonRepository personRepository,
            InstructorRepository instructorRepository,
            LearnerRepository learnerRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.personRepository = personRepository;
        this.instructorRepository = instructorRepository;
        this.learnerRepository = learnerRepository;
        this.passwordEncoder = passwordEncoder;
    }
    //#endregion

    //#region Event Handling
    /**
     * Handles the application context refreshed event to seed initial data.
     *
     * @param event the context refreshed event
     */
    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        this.seedUsers();
    }
    //#endregion

    //#region Seeding Logic

    private void seedUsers() {
        List<PersonData> usersToCreate = createUserDataList();

        for (PersonData userData : usersToCreate) {
            Optional<Person> existingPerson = personRepository.findByEmail(userData.email);

            if (existingPerson.isPresent()) {
                continue;
            }

            Person person = createPerson(userData);
            Person savedPerson = personRepository.save(person);

            if (userData.isInstructor) {
                createInstructor(savedPerson, userData);
            } else {
                createLearner(savedPerson, userData);
            }
        }
    }

    /**
     * Creates a Person entity from PersonData.
     *
     * @param data the person data
     * @return the created Person entity
     */
    private Person createPerson(PersonData data) {
        Person person = new Person();
        person.setEmail(data.email);
        person.setPasswordHash(passwordEncoder.encode(data.password));
        person.setFullName(data.fullName);
        person.setProfilePhotoUrl(data.profilePhotoUrl);
        person.setPreferredLanguage(data.preferredLanguage);
        person.setGoogleOauthId(data.googleOauthId);
        person.setEmailVerified(data.emailVerified);
        person.setActive(data.active);
        return person;
    }

    /**
     * Creates an Instructor entity linked to a Person.
     *
     * @param person the person entity
     * @param data the person data containing instructor information
     */
    private void createInstructor(Person person, PersonData data) {
        Instructor instructor = new Instructor();
        instructor.setPerson(person);
        instructor.setPaypalAccount(data.paypalAccount);
        instructor.setSkillcoinsBalance(data.instructorSkillcoins);
        instructor.setVerifiedAccount(data.instructorVerified);
        instructor.setAverageRating(data.averageRating);
        instructor.setSessionsTaught(data.sessionsTaught);
        instructor.setTotalEarnings(data.totalEarnings);
        instructor.setBiography(data.biography);
        instructorRepository.save(instructor);
    }

    /**
     * Creates a Learner entity linked to a Person.
     *
     * @param person the person entity
     * @param data the person data containing learner information
     */
    private void createLearner(Person person, PersonData data) {
        Learner learner = new Learner();
        learner.setPerson(person);
        learner.setSkillcoinsBalance(data.learnerSkillcoins);
        learner.setCompletedSessions(data.completedSessions);
        learner.setCredentialsObtained(data.credentialsObtained);
        learnerRepository.save(learner);
    }

    /**
     * Creates the list of test user data to be seeded.
     *
     * @return list of PersonData objects
     */
    private List<PersonData> createUserDataList() {
        List<PersonData> users = new ArrayList<>();

        users.add(new PersonData(
                "maria.rodriguez@skillswap.com", "Password123!", "María Rodríguez",
                "https://i.pravatar.cc/150?img=1", "es", null, true, true,
                true, "maria.paypal@example.com", new BigDecimal("180.00"),
                true, new BigDecimal("4.9"), 32, new BigDecimal("3200.00"),
                "Instructora certificada de español con más de 5 años enseñando a extranjeros. Especializada en conversación, gramática y cultura hispanoamericana. Clases dinámicas y personalizadas."
        ));
        users.add(new PersonData(
                "john.smith@skillswap.com", "Password123!!", "John Smith",
                "https://i.pravatar.cc/150?img=2", "en", null, true, true,
                false, new BigDecimal("250.00"), 15, 5
        ));
        users.add(new PersonData(
                "sophie.chen@skillswap.com", "Password123!", "Sophie Chen",
                "https://i.pravatar.cc/150?img=3", "en", null, true, true,
                true, "sophie.paypal@example.com", new BigDecimal("380.00"),
                true, new BigDecimal("4.9"), 68, new BigDecimal("7200.00"),
                "Desarrolladora senior con 8 años de experiencia en Python, JavaScript y React. Mentora en bootcamps y empresas Fortune 500. Enfocada en código limpio y arquitectura escalable."
        ));
        users.add(new PersonData(
                "lucas.santos@skillswap.com", "Password123!", "Lucas Santos",
                "https://i.pravatar.cc/150?img=4", "pt", null, false, true,
                true, "lucas.paypal@example.com", new BigDecimal("90.00"),
                false, new BigDecimal("4.6"), 14, new BigDecimal("1100.00"),
                "Músico profesional y profesor de guitarra con 6 años de experiencia. Especializado en estilos latinos, rock y fingerstyle. Clases prácticas y divertidas."
        ));;

        users.add(new PersonData(
                "emma.wilson@skillswap.com", "Password123!", "Emma Wilson",
                "https://i.pravatar.cc/150?img=5", "en", null, false, true,
                false, new BigDecimal("150.00"), 8, 2
        ));

        users.add(new PersonData(
                "ahmed.hassan@skillswap.com", "Password123!", "Ahmed Hassan",
                "https://i.pravatar.cc/150?img=6", "ar", null, true, true,
                true, "ahmed.paypal@example.com", new BigDecimal("400.00"),
                true, new BigDecimal("4.7"), 35, new BigDecimal("3500.00"),
                "Specialized in data science and machine learning tutorials."
        ));

        users.add(new PersonData(
                "ana.garcia@skillswap.com", "Password123!", "Ana García",
                "https://i.pravatar.cc/150?img=7", "es", null, true, false,
                true, "ana.paypal@example.com", new BigDecimal("100.00"),
                false, new BigDecimal("4.6"), 15, new BigDecimal("1200.00"),
                "Teaching photography and digital editing skills."
        ));

        users.add(new PersonData(
                "oliver.brown@skillswap.com", "Password123!", "Oliver Brown",
                "https://i.pravatar.cc/150?img=8", "en", "google_oauth_oliver123", true, true,
                false, new BigDecimal("300.00"), 25, 10
        ));

        users.add(new PersonData(
                "yuki.tanaka@skillswap.com", "Password123!", "Yuki Tanaka",
                "https://i.pravatar.cc/150?img=9", "ja", null, true, true,
                true, "yuki.paypal@example.com", new BigDecimal("620.00"),
                true, new BigDecimal("5.0"), 78, new BigDecimal("8200.00"),
                "Maestra de arte japonés tradicional: caligrafía, sumi-e, origami e ikebana. Certificada en Kioto. Clases en japonés e inglés con enfoque cultural profundo."
        ));

        users.add(new PersonData(
                "isabella.rossi@skillswap.com", "Password123!", "Isabella Rossi",
                "https://i.pravatar.cc/150?img=10", "it", null, false, true,
                true, "isabella.paypal@example.com", new BigDecimal("75.00"),
                false, new BigDecimal("4.4"), 8, new BigDecimal("600.00"),
                "Italian cuisine and cooking classes specialist."
        ));

        users.add(new PersonData(
                "david.kim@skillswap.com", "Password123!", "David Kim",
                "https://i.pravatar.cc/150?img=11", "ko", "google_oauth_david456", true, true,
                true, "david.paypal@example.com", new BigDecimal("350.00"),
                true, new BigDecimal("4.8"), 40, new BigDecimal("4000.00"),
                "Software architecture and system design mentor."
        ));

        users.add(new PersonData(
                "carlos.dela@skillswap.com", "Password123!", "Carlos Delao",
                "https://i.pravatar.cc/150?img=11", "es", "google_oauth_carlos506", true, true,
                true, "carlosd.paypal@example.com", new BigDecimal("450.00"),
                true, new BigDecimal("4.8"), 52, new BigDecimal("5800.00"),
                "Ingeniero de software senior especializado en arquitectura de microservicios, DevOps y machine learning. Mentor en startups y empresas tecnológicas."
        ));

        users.add(new PersonData(
                "natalie.blanc@skillswap.com", "Password123!", "Natalie Blanc",
                "https://i.pravatar.cc/150?img=12", "fr", null, true, true,
                false, new BigDecimal("220.00"), 18, 7
        ));

        users.add(new PersonData(
                "marco.silva@skillswap.com", "Password123!", "Marco Silva",
                "https://i.pravatar.cc/150?img=13", "pt", null, false, true,
                true, "marco.paypal@example.com", new BigDecimal("125.00"),
                false, new BigDecimal("4.3"), 12, new BigDecimal("1000.00"),
                "Fitness trainer specializing in yoga and meditation."
        ));

        users.add(new PersonData(
                "sara.anderson@skillswap.com", "Password123!", "Sara Anderson",
                "https://i.pravatar.cc/150?img=14", "en", null, false, false,
                false, new BigDecimal("100.00"), 5, 1
        ));

        users.add(new PersonData(
                "miguel.lopez@skillswap.com", "Password123!", "Miguel López",
                "https://i.pravatar.cc/150?img=15", "es", "google_oauth_miguel789", true, true,
                true, "miguel.paypal@example.com", new BigDecimal("275.00"),
                true, new BigDecimal("4.7"), 30, new BigDecimal("3000.00"),
                "Digital marketing and SEO strategies instructor."
        ));

        users.add(new PersonData(
                "elena.popov@skillswap.com", "Password123!", "Elena Popov",
                "https://i.pravatar.cc/150?img=16", "ru", null, true, true,
                true, "elena.paypal@example.com", new BigDecimal("200.00"),
                true, new BigDecimal("4.9"), 45, new BigDecimal("4500.00"),
                "Mathematics and physics tutor for all levels."
        ));

        users.add(new PersonData(
                "james.taylor@skillswap.com", "Password123!", "James Taylor",
                "https://i.pravatar.cc/150?img=17", "en", null, false, true,
                false, new BigDecimal("180.00"), 14, 6
        ));

        users.add(new PersonData(
                "li.wei@skillswap.com", "Password123!", "Li Wei",
                "https://i.pravatar.cc/150?img=18", "zh", null, true, true,
                true, "li.paypal@example.com", new BigDecimal("450.00"),
                true, new BigDecimal("4.8"), 55, new BigDecimal("5500.00"),
                "Mandarin language teacher and cultural consultant."
        ));

        users.add(new PersonData(
                "clara.mueller@skillswap.com", "Password123!", "Clara Müller",
                "https://i.pravatar.cc/150?img=19", "de", null, true, true,
                true, "clara.paypal@example.com", new BigDecimal("180.00"),
                false, new BigDecimal("4.5"), 18, new BigDecimal("1800.00"),
                "Graphic design and UI/UX principles educator."
        ));

        users.add(new PersonData(
                "thomas.martin@skillswap.com", "Password123!", "Thomas Martin",
                "https://i.pravatar.cc/150?img=20", "en", "google_oauth_thomas321", false, true,
                false, new BigDecimal("90.00"), 3, 0
        ));

        return users;
    }
    //#endregion

    //#region Inner Class
    /**
     * Data class holding information for creating test users.
     */
    private static class PersonData {
        String email;
        String password;
        String fullName;
        String profilePhotoUrl;
        String preferredLanguage;
        String googleOauthId;
        Boolean emailVerified;
        Boolean active;
        boolean isInstructor;
        String paypalAccount;
        BigDecimal instructorSkillcoins;
        Boolean instructorVerified;
        BigDecimal averageRating;
        Integer sessionsTaught;
        BigDecimal totalEarnings;
        String biography;
        BigDecimal learnerSkillcoins;
        Integer completedSessions;
        Integer credentialsObtained;

        PersonData(String email, String password, String fullName, String profilePhotoUrl,
                   String preferredLanguage, String googleOauthId, Boolean emailVerified, Boolean active,
                   boolean isInstructor, String paypalAccount, BigDecimal instructorSkillcoins,
                   Boolean instructorVerified, BigDecimal averageRating, Integer sessionsTaught,
                   BigDecimal totalEarnings, String biography) {
            this.email = email;
            this.password = password;
            this.fullName = fullName;
            this.profilePhotoUrl = profilePhotoUrl;
            this.preferredLanguage = preferredLanguage;
            this.googleOauthId = googleOauthId;
            this.emailVerified = emailVerified;
            this.active = active;
            this.isInstructor = isInstructor;
            this.paypalAccount = paypalAccount;
            this.instructorSkillcoins = instructorSkillcoins;
            this.instructorVerified = instructorVerified;
            this.averageRating = averageRating;
            this.sessionsTaught = sessionsTaught;
            this.totalEarnings = totalEarnings;
            this.biography = biography;
        }

        PersonData(String email, String password, String fullName, String profilePhotoUrl,
                   String preferredLanguage, String googleOauthId, Boolean emailVerified, Boolean active,
                   boolean isInstructor, BigDecimal learnerSkillcoins,
                   Integer completedSessions, Integer credentialsObtained) {
            this.email = email;
            this.password = password;
            this.fullName = fullName;
            this.profilePhotoUrl = profilePhotoUrl;
            this.preferredLanguage = preferredLanguage;
            this.googleOauthId = googleOauthId;
            this.emailVerified = emailVerified;
            this.active = active;
            this.isInstructor = isInstructor;
            this.learnerSkillcoins = learnerSkillcoins;
            this.completedSessions = completedSessions;
            this.credentialsObtained = credentialsObtained;
        }
    }
    //#endregion
}