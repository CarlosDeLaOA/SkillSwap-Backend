package com.project.skillswap.logic.entity.Notification;

import com.project.skillswap.logic.entity.Person.Person;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Date;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * Cuenta las notificaciones de un tipo específico enviadas después de una fecha.
     */
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.person = :person AND n.type = :type AND n.sendDate >= :date")
    long countByPersonAndTypeAndSendDateAfter(
            @Param("person") Person person,
            @Param("type") NotificationType type,
            @Param("date") Date date
    );

    /**
     * Obtiene las notificaciones de una persona ordenadas por fecha
     */
    List<Notification> findByPersonOrderBySendDateDesc(Person person);

    /**
     * Obtiene las notificaciones no leídas de una persona
     */
    @Query("SELECT n FROM Notification n WHERE n.person = :person AND n.read = false ORDER BY n.sendDate DESC")
    List<Notification> findUnreadByPerson(@Param("person") Person person);

    /**
     * Cuenta las notificaciones no leídas de una persona
     */
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.person = :person AND n.read = false")
    long countUnreadByPerson(@Param("person") Person person);

    /**
     * Obtiene notificaciones no leídas de todas las personas (para digest diario)
     */
    @Query("SELECT n FROM Notification n WHERE n.read = false AND n.type = 'IN_APP' AND n.sendDate < :before")
    List<Notification> findUnreadBeforeDate(@Param("before") Date before);
}