package com. project.skillswap.logic.entity.GroupSessionDocument;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind. annotation.ResponseStatus;

/**
 * Excepción lanzada cuando un usuario intenta acceder a documentos sin ser miembro del grupo.
 */
@ResponseStatus(HttpStatus.FORBIDDEN)
public class DocumentAccessDeniedException extends RuntimeException {

    //#region Fields
    private final Long personId;
    private final Long communityId;
    private final String action;
    //#endregion

    //#region Constructors
    public DocumentAccessDeniedException(String message) {
        super(message);
        this.personId = null;
        this. communityId = null;
        this. action = null;
    }

    public DocumentAccessDeniedException(Long personId, Long communityId, String action) {
        super(String.format(
                "Acceso denegado: el usuario %d no tiene permisos para %s documentos en el grupo %d.  Solo los miembros pueden acceder.",
                personId, action, communityId));
        this. personId = personId;
        this. communityId = communityId;
        this.action = action;
    }

    public DocumentAccessDeniedException(String message, Throwable cause) {
        super(message, cause);
        this.personId = null;
        this.communityId = null;
        this.action = null;
    }
    //#endregion

    //#region Getters
    public Long getPersonId() {
        return personId;
    }

    public Long getCommunityId() {
        return communityId;
    }

    public String getAction() {
        return action;
    }
    //#endregion

    //#region Factory Methods
    /**
     * Crea excepción para acción de subir documento.
     */
    public static DocumentAccessDeniedException forUpload(Long personId, Long communityId) {
        return new DocumentAccessDeniedException(personId, communityId, "subir");
    }

    /**
     * Crea excepción para acción de descargar documento.
     */
    public static DocumentAccessDeniedException forDownload(Long personId, Long communityId) {
        return new DocumentAccessDeniedException(personId, communityId, "descargar");
    }

    /**
     * Crea excepción para acción de visualizar documentos.
     */
    public static DocumentAccessDeniedException forView(Long personId, Long communityId) {
        return new DocumentAccessDeniedException(personId, communityId, "visualizar");
    }

    /**
     * Crea excepción para acción de eliminar documento.
     */
    public static DocumentAccessDeniedException forDelete(Long personId, Long communityId) {
        return new DocumentAccessDeniedException(personId, communityId, "eliminar");
    }
    //#endregion
}