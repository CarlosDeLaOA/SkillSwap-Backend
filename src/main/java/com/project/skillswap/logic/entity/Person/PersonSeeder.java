package com.project.skillswap.logic.entity.Person;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
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
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Order(4)
@Component
public class PersonSeeder implements ApplicationListener<ContextRefreshedEvent> {
    private static final Logger logger = LoggerFactory.getLogger(PersonSeeder.class);

    private final PersonRepository personRepository;
    private final InstructorRepository instructorRepository;
    private final LearnerRepository learnerRepository;
    private final PasswordEncoder passwordEncoder;
    private final Random random = new Random();

    public PersonSeeder(PersonRepository personRepository,
                        InstructorRepository instructorRepository,
                        LearnerRepository learnerRepository,
                        PasswordEncoder passwordEncoder) {
        this.personRepository = personRepository;
        this.instructorRepository = instructorRepository;
        this.learnerRepository = learnerRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        this.seedPersons();
    }

    private void seedPersons() {
        List<PersonData> personsToCreate = createPersonDataList();

        for (PersonData personData : personsToCreate) {
            Optional<Person> existingPerson = personRepository.findByEmail(personData.email);
            if (existingPerson.isPresent()) continue;

            Person person = new Person();
            person.setEmail(personData.email);
            person.setPasswordHash(passwordEncoder.encode("Password123!"));
            person.setFullName(personData.fullName);
            person.setProfilePhotoUrl(personData.profilePhotoUrl);
            person.setPreferredLanguage("es");
            person.setEmailVerified(true);
            person.setActive(true);
            person.setRegistrationDate(personData.registrationDate);
            person.setLastConnection(generateRecentDate());

            person = personRepository.save(person);

            // Crear SOLO Instructor O Learner (no ambos)
            if (personData.isInstructor) {
                createInstructor(person, personData);
            } else if (personData.isLearner) {
                createLearner(person, personData);
            }
        }
    }

    private void createInstructor(Person person, PersonData personData) {
        Instructor instructor = new Instructor();
        instructor.setPerson(person);
        instructor.setPaypalAccount(personData.email.replace("@", "_paypal@"));
        instructor.setSkillcoinsBalance(personData.instructorBalance);
        instructor.setVerifiedAccount(personData.verifiedInstructor);
        instructor.setAverageRating(personData.averageRating);
        instructor.setSessionsTaught(personData.sessionsTaught);
        instructor.setTotalEarnings(personData.totalEarnings);
        instructor.setBiography(personData.biography);
        instructorRepository.save(instructor);
    }

    private void createLearner(Person person, PersonData personData) {
        Learner learner = new Learner();
        learner.setPerson(person);
        learner.setSkillcoinsBalance(personData.learnerBalance);
        learner.setCompletedSessions(personData.completedSessions);
        learner.setCredentialsObtained(personData.credentialsObtained);
        learnerRepository.save(learner);
    }

    private List<PersonData> createPersonDataList() {
        List<PersonData> persons = new ArrayList<>();

        // ========== USUARIOS ESPECÍFICOS ==========

        // 1. Camila Morales - LEARNER
        persons.add(new PersonData(
                "cmoralesso@ucenfotec.ac.cr",
                "Camila Morales",
                "https://i.pravatar.cc/150?img=1",
                getDateMonthsAgo(6),
                false, true,
                BigDecimal.ZERO, BigDecimal.valueOf(450.00),
                false, BigDecimal.ZERO, 0, BigDecimal.ZERO,
                null,
                85, 65
        ));

        // 2. Mia Solano - LEARNER
        persons.add(new PersonData(
                "moralescamila500@outlook.com",
                "Mia Solano",
                "https://i.pravatar.cc/150?img=5",
                getDateMonthsAgo(5),
                false, true,
                BigDecimal.ZERO, BigDecimal.valueOf(320.00),
                false, BigDecimal.ZERO, 0, BigDecimal.ZERO,
                null,
                72, 62
        ));

        // 3. Mila Morales - INSTRUCTOR
        persons.add(new PersonData(
                "moralescamila500@gmail.com",
                "Mila Morales",
                "https://i.pravatar.cc/150?img=9",
                getDateMonthsAgo(6),
                true, false,
                BigDecimal.valueOf(850.00), BigDecimal.ZERO,
                true, BigDecimal.valueOf(4.7), 145, BigDecimal.valueOf(2800.00),
                "Instructora apasionada por la enseñanza de idiomas y programación. Con más de 5 años de experiencia ayudando a estudiantes a alcanzar sus metas.",
                0, 0
        ));

        // 4. Sammy Toruño - LEARNER
        persons.add(new PersonData(
                "storunos@ucenfotec.ac.cr",
                "Sammy Toruño",
                "https://i.pravatar.cc/150?img=12",
                getDateMonthsAgo(5),
                false, true,
                BigDecimal.ZERO, BigDecimal.valueOf(380.00),
                false, BigDecimal.ZERO, 0, BigDecimal.ZERO,
                null,
                78, 68
        ));

        // 5. Carlos Dela - INSTRUCTOR
        persons.add(new PersonData(
                "cdelaoa@ucenfotec.ac.cr",
                "Carlos Dela",
                "https://i.pravatar.cc/150?img=13",
                getDateMonthsAgo(4),
                true, false,
                BigDecimal.valueOf(670.00), BigDecimal.ZERO,
                true, BigDecimal.valueOf(4.6), 112, BigDecimal.valueOf(2100.00),
                "Experto en arte digital y diseño gráfico. Me encanta compartir conocimientos creativos con personas apasionadas.",
                0, 0
        ));

        // 6. Jose Mario Arias - INSTRUCTOR
        persons.add(new PersonData(
                "jmariasm@ucenfotec.ac.cr",
                "Jose Mario Arias",
                "https://i.pravatar.cc/150?img=14",
                getDateMonthsAgo(6),
                true, false,
                BigDecimal.valueOf(920.00), BigDecimal.ZERO,
                true, BigDecimal.valueOf(4.8), 156, BigDecimal.valueOf(3200.00),
                "Chef profesional especializado en cocina internacional. Comparto mi amor por la gastronomía con entusiasmo.",
                0, 0
        ));

        // 7. Esteban Rojas - LEARNER
        persons.add(new PersonData(
                "erojash@ucenfotec.ac.cr",
                "Esteban Rojas",
                "https://i.pravatar.cc/150?img=15",
                getDateMonthsAgo(3),
                false, true,
                BigDecimal.ZERO, BigDecimal.valueOf(410.00),
                false, BigDecimal.ZERO, 0, BigDecimal.ZERO,
                null,
                63, 60
        ));

        // ========== USUARIOS ADICIONALES FICTICIOS ==========

        String[] firstNames = {
                "Andrea", "Roberto", "María", "Daniel", "Laura", "Fernando", "Sofía", "Miguel",
                "Valentina", "Diego", "Isabella", "Alejandro", "Catalina", "Pablo", "Gabriela", "Andrés",
                "Valeria", "Ricardo", "Natalia", "Eduardo", "Carolina", "Felipe", "Mariana", "Javier",
                "Daniela", "Sebastián", "Paulina", "Luis", "Nicole", "Mauricio", "Juliana", "Oscar",
                "Melissa", "Jorge", "Adriana", "Raúl", "Camilo", "Paola", "Arturo", "Ana",
                "Rodrigo", "Lucía", "Hernán", "Patricia", "Marco", "Silvia", "Enrique", "Verónica",
                "Alberto", "Mónica", "Guillermo", "Cristina", "Esteban"
        };

        String[] lastNames = {
                "González", "Rodríguez", "Fernández", "López", "Martínez", "Sánchez", "Pérez", "Gómez",
                "Ramírez", "Torres", "Flores", "Rivera", "Castillo", "Vargas", "Herrera", "Jiménez",
                "Moreno", "Álvarez", "Romero", "Castro", "Ortiz", "Silva", "Gutiérrez", "Núñez",
                "Mendoza", "Cruz", "Chávez", "Reyes", "Vega", "Campos", "Soto", "Medina",
                "Aguilar", "Ruiz", "Cabrera", "León", "Navarro", "Ibarra", "Cortés", "Ramos",
                "Bravo", "Contreras", "Muñoz", "Vásquez", "Salazar", "Guerrero", "Delgado", "Rojas",
                "Palacios", "Valencia", "Mora", "Espinoza", "Acosta"
        };

        String[] biographies = {
                "Apasionado por la enseñanza y el desarrollo personal continuo.",
                "Experto en mi campo con años de experiencia práctica.",
                "Me encanta compartir conocimientos y ayudar a otros a crecer.",
                "Profesional dedicado al aprendizaje y la mejora constante.",
                "Entusiasta de las nuevas tecnologías y metodologías de enseñanza.",
                "Creo firmemente en el poder del intercambio de conocimientos.",
                "Mi misión es hacer el aprendizaje accesible y divertido.",
                "Especialista comprometido con la excelencia educativa.",
                "Educador con pasión por la innovación y la creatividad.",
                "Mentor experimentado enfocado en resultados tangibles."
        };

        for (int i = 0; i < 53; i++) {
            String firstName = firstNames[i % firstNames.length];
            String lastName = lastNames[i % lastNames.length];
            String fullName = firstName + " " + lastName;
            String email = firstName.toLowerCase() + "." + lastName.toLowerCase() + i + "@skillswap.com";

            // Distribución: 50% instructores, 50% learners
            boolean isInstructor = i % 2 == 0;
            boolean isLearner = !isInstructor;

            BigDecimal instructorBalance = isInstructor ? BigDecimal.valueOf(200 + random.nextInt(800)) : BigDecimal.ZERO;
            BigDecimal learnerBalance = isLearner ? BigDecimal.valueOf(100 + random.nextInt(400)) : BigDecimal.ZERO;

            boolean verified = isInstructor && random.nextBoolean();
            BigDecimal rating = isInstructor ? BigDecimal.valueOf(3.5 + random.nextDouble() * 1.5).setScale(2, BigDecimal.ROUND_HALF_UP) : BigDecimal.ZERO;
            int sessionsTaught = isInstructor ? 30 + random.nextInt(120) : 0;
            BigDecimal earnings = isInstructor ? BigDecimal.valueOf(sessionsTaught * (15 + random.nextInt(20))) : BigDecimal.ZERO;

            String bio = isInstructor ? biographies[i % biographies.length] : null;

            int completedSessions = isLearner ? 25 + random.nextInt(80) : 0;
            int credentials = isLearner ? 60 + random.nextInt(20) : 0;

            persons.add(new PersonData(
                    email,
                    fullName,
                    "https://i.pravatar.cc/150?img=" + (20 + i),
                    getDateMonthsAgo(random.nextInt(6) + 1),
                    isInstructor, isLearner,
                    instructorBalance, learnerBalance,
                    verified, rating, sessionsTaught, earnings,
                    bio,
                    completedSessions, credentials
            ));
        }

        return persons;
    }

    private Date getDateMonthsAgo(int months) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MONTH, -months);
        cal.add(Calendar.DAY_OF_MONTH, -random.nextInt(15));
        return cal.getTime();
    }

    private Date generateRecentDate() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.HOUR, -random.nextInt(72));
        return cal.getTime();
    }

    private static class PersonData {
        String email;
        String fullName;
        String profilePhotoUrl;
        Date registrationDate;
        boolean isInstructor;
        boolean isLearner;
        BigDecimal instructorBalance;
        BigDecimal learnerBalance;
        boolean verifiedInstructor;
        BigDecimal averageRating;
        int sessionsTaught;
        BigDecimal totalEarnings;
        String biography;
        int completedSessions;
        int credentialsObtained;

        PersonData(String email, String fullName, String profilePhotoUrl, Date registrationDate,
                   boolean isInstructor, boolean isLearner,
                   BigDecimal instructorBalance, BigDecimal learnerBalance,
                   boolean verifiedInstructor, BigDecimal averageRating, int sessionsTaught, BigDecimal totalEarnings,
                   String biography,
                   int completedSessions, int credentialsObtained) {
            this.email = email;
            this.fullName = fullName;
            this.profilePhotoUrl = profilePhotoUrl;
            this.registrationDate = registrationDate;
            this.isInstructor = isInstructor;
            this.isLearner = isLearner;
            this.instructorBalance = instructorBalance;
            this.learnerBalance = learnerBalance;
            this.verifiedInstructor = verifiedInstructor;
            this.averageRating = averageRating;
            this.sessionsTaught = sessionsTaught;
            this.totalEarnings = totalEarnings;
            this.biography = biography;
            this.completedSessions = completedSessions;
            this.credentialsObtained = credentialsObtained;
        }
    }
}