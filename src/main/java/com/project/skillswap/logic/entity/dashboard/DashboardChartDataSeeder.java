package com.project.skillswap.logic.entity.dashboard;

import com.project.skillswap.logic.entity.Booking.Booking;
import com.project.skillswap.logic.entity.Booking.BookingRepository;
import com.project.skillswap.logic.entity.Credential.Credential;
import com.project.skillswap.logic.entity.Credential.CredentialRepository;
import com.project.skillswap.logic.entity.LearningSession.LearningSession;
import com.project.skillswap.logic.entity.LearningSession.LearningSessionRepository;
import com.project.skillswap.logic.entity.LearningSession.SessionStatus;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Random;

/**
 * Seeder para distribuir TODOS los datos en los últimos 4 meses
 * Order 11 para ejecutarse DESPUÉS de todos los seeders existentes
 */
@Order(11)
@Component
public class DashboardChartDataSeeder implements ApplicationListener<ContextRefreshedEvent> {

    private final CredentialRepository credentialRepository;
    private final LearningSessionRepository learningSessionRepository;
    private final BookingRepository bookingRepository;
    private final Random random = new Random();

    public DashboardChartDataSeeder(
            CredentialRepository credentialRepository,
            LearningSessionRepository learningSessionRepository,
            BookingRepository bookingRepository) {
        this.credentialRepository = credentialRepository;
        this.learningSessionRepository = learningSessionRepository;
        this.bookingRepository = bookingRepository;
    }

    @Override
    @Transactional
    public void onApplicationEvent(ContextRefreshedEvent event) {
        System.out.println("\n📊 ========================================");
        System.out.println("📊 FORZANDO distribución de datos para gráficas");
        System.out.println("📊 ========================================");

        distribuirTodasLasCredenciales();
        actualizarTodosLosBookings();

        System.out.println("📊 ========================================");
        System.out.println("✅ Distribución FORZADA completada");
        System.out.println("📊 ========================================\n");
    }

    /**
     * Distribuye TODAS las credenciales en los últimos 4 meses
     * SIN IMPORTAR si ya tienen fecha
     */
    private void distribuirTodasLasCredenciales() {
        System.out.println("\n📅 DISTRIBUYENDO TODAS LAS CREDENCIALES...");

        List<Credential> credentials = credentialRepository.findAll();

        if (credentials.isEmpty()) {
            System.out.println("⚠️ No hay credenciales en la base de datos");
            return;
        }

        System.out.println("📚 Total credenciales encontradas: " + credentials.size());

        LocalDateTime now = LocalDateTime.now();
        int actualizadas = 0;

        for (Credential credential : credentials) {
            // Distribuir TODAS en los últimos 4 meses
            int mesAtras = random.nextInt(4); // 0-3 meses
            int diaAtras = random.nextInt(28) + 1; // 1-28 días

            LocalDateTime nuevaFecha = now.minusMonths(mesAtras).minusDays(diaAtras);
            credential.setObtainedDate(toDate(nuevaFecha));
            credentialRepository.save(credential);
            actualizadas++;

            if (actualizadas % 10 == 0) {
                System.out.println("  ⏳ Procesadas " + actualizadas + " credenciales...");
            }
        }

        System.out.println("✅ TODAS las " + actualizadas + " credenciales ahora tienen fechas en los últimos 4 meses");

        // Mostrar distribución
        mostrarDistribucionCredenciales();
    }

    /**
     * Actualiza TODOS los bookings con attended
     */
    private void actualizarTodosLosBookings() {
        System.out.println("\n👥 ACTUALIZANDO TODOS LOS BOOKINGS...");

        List<LearningSession> sessionsPasadas = learningSessionRepository
                .findByStatus(SessionStatus.FINISHED);

        if (sessionsPasadas.isEmpty()) {
            System.out.println("⚠️ No hay sesiones finalizadas");
            return;
        }

        System.out.println("📚 Total sesiones FINISHED: " + sessionsPasadas.size());

        int bookingsActualizados = 0;
        int totalBookings = 0;

        for (LearningSession session : sessionsPasadas) {
            List<Booking> bookings = bookingRepository.findByLearningSessionId(session.getId());
            totalBookings += bookings.size();

            for (Booking booking : bookings) {
                // Actualizar TODOS, sin importar si ya tenían attended
                boolean asistio = random.nextDouble() < (0.70 + random.nextDouble() * 0.20);
                booking.setAttended(asistio);
                bookingRepository.save(booking);
                bookingsActualizados++;
            }
        }

        System.out.println("✅ TODOS los " + bookingsActualizados + " bookings ahora tienen attended definido");

        // Mostrar distribución
        mostrarDistribucionAsistencia();
    }

    /**
     * Muestra la distribución de credenciales por mes
     */
    private void mostrarDistribucionCredenciales() {
        System.out.println("\n📊 Distribución de Credenciales por Mes:");

        LocalDateTime now = LocalDateTime.now();

        for (int i = 3; i >= 0; i--) {
            LocalDateTime mesActual = now.minusMonths(i);
            String nombreMes = mesActual.getMonth().name().substring(0, 3);

            long count = credentialRepository.findAll().stream()
                    .filter(c -> c.getObtainedDate() != null)
                    .filter(c -> {
                        LocalDateTime fechaCred = c.getObtainedDate()
                                .toInstant()
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                                .atStartOfDay();
                        return fechaCred.getYear() == mesActual.getYear() &&
                                fechaCred.getMonth() == mesActual.getMonth();
                    })
                    .count();

            System.out.println("  - " + nombreMes + ": " + count + " credenciales");
        }
    }

    /**
     * Muestra la distribución de asistencia
     */
    private void mostrarDistribucionAsistencia() {
        System.out.println("\n📊 Distribución de Asistencia:");

        List<Booking> allBookings = bookingRepository.findAll();

        long presentes = allBookings.stream()
                .filter(b -> b.getAttended() != null && b.getAttended())
                .count();

        long ausentes = allBookings.stream()
                .filter(b -> b.getAttended() != null && !b.getAttended())
                .count();

        System.out.println("  - Presentes: " + presentes);
        System.out.println("  - Ausentes: " + ausentes);
        System.out.println("  - Total: " + (presentes + ausentes));
    }

    /**
     * Convierte LocalDateTime a Date
     */
    private Date toDate(LocalDateTime localDateTime) {
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }
}