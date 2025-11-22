package com.project.skillswap.logic.entity.LearningSession.persistence;

import com.project.skillswap.logic.entity.LearningSession.LearningSession;
import org.springframework.stereotype.Component;

/**
 * Componente responsable de validar la persistencia de sesiones en BD
 * Verifica que el guardado fue exitoso y completo
 * Validar guardado en base de datos interna
 */
@Component
public class SessionPersistenceValidator {

    //#region Public Methods
    /**
     * Valida que una sesión fue guardada correctamente en la BD
     * Verifica que tenga ID y estado válido
     *
     * @param session Sesión a validar
     * @return true si fue guardada correctamente
     * @throws IllegalArgumentException Si no fue guardada correctamente
     */
    public boolean validateSessionSavedCorrectly(LearningSession session) {
        if (session == null) {
            throw new IllegalArgumentException("La sesión es nula");
        }

        if (session.getId() == null) {
            System.err.println("⚠️ [SessionPersistenceValidator] Error: Sesión guardada sin ID");
            throw new IllegalArgumentException("Sesión guardada sin ID válido en BD");
        }

        if (session.getStatus() == null) {
            System.err.println("⚠️ [SessionPersistenceValidator] Error: Sesión guardada sin estado");
            throw new IllegalArgumentException("Sesión guardada sin estado válido en BD");
        }

        System.out.println(" [SessionPersistenceValidator] ✅ Sesión validada correctamente");
        return true;
    }

    /**
     * Valida que la sesión tiene fecha y hora programada
     *
     * @param session Sesión a validar
     * @return true si tiene fecha/hora
     */
    public boolean validateSessionHasDateTime(LearningSession session) {
        if (session.getScheduledDatetime() == null) {
            throw new IllegalArgumentException("Sesión sin fecha/hora programada");
        }
        return true;
    }

    /**
     * Valida que la sesión tiene instructor asignado
     *
     * @param session Sesión a validar
     * @return true si tiene instructor
     */
    public boolean validateSessionHasInstructor(LearningSession session) {
        if (session.getInstructor() == null || session.getInstructor().getId() == null) {
            throw new IllegalArgumentException("Sesión sin instructor asignado");
        }
        return true;
    }
    //#endregion
}