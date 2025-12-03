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
}