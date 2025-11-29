package com.project.skillswap.logic.entity.GroupSessionDocument;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Excepción lanzada cuando se intenta subir un documento con formato inválido.
 * Solo se permiten archivos PDF.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidDocumentFormatException extends RuntimeException {

    //#region Fields
    private final String fileName;
    private final String contentType;
    //#endregion

    //#region Constructors
    public InvalidDocumentFormatException(String message) {
        super(message);
        this.fileName = null;
        this.contentType = null;
    }

    public InvalidDocumentFormatException(String fileName, String contentType) {
        super(String.format("Formato de archivo inválido: '%s' (tipo: %s). Solo se permiten archivos PDF.",
                fileName, contentType));
        this.fileName = fileName;
        this.contentType = contentType;
    }

    public InvalidDocumentFormatException(String message, Throwable cause) {
        super(message, cause);
        this.fileName = null;
        this.contentType = null;
    }
    //#endregion

    //#region Getters
    public String getFileName() {
        return fileName;
    }

    public String getContentType() {
        return contentType;
    }
    //#endregion
}