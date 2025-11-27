package com.project.skillswap.logic.entity.GroupSessionDocument;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Excepción lanzada cuando se excede el límite de almacenamiento del grupo (100MB).
 */
@ResponseStatus(HttpStatus. PAYLOAD_TOO_LARGE)
public class StorageLimitExceededException extends RuntimeException {

    //#region Constants
    public static final long MAX_STORAGE_BYTES = 100L * 1024L * 1024L; // 100MB
    //#endregion

    //#region Fields
    private final Long currentStorageBytes;
    private final Long fileSize;
    private final Long communityId;
    //#endregion

    //#region Constructors
    public StorageLimitExceededException(String message) {
        super(message);
        this.currentStorageBytes = null;
        this.fileSize = null;
        this.communityId = null;
    }

    public StorageLimitExceededException(Long communityId, Long currentStorageBytes, Long fileSize) {
        super(String.format(
                        "Límite de almacenamiento excedido para el grupo %d.  " +
                                "Almacenamiento actual: %.2f MB, Archivo: %.2f MB, Límite: 100 MB.",
                        communityId,
                        currentStorageBytes / (1024.0 * 1024.0),
                fileSize / (1024.0 * 1024.0)));
        this.communityId = communityId;
        this.currentStorageBytes = currentStorageBytes;
        this. fileSize = fileSize;
    }

    public StorageLimitExceededException(String message, Throwable cause) {
        super(message, cause);
        this.currentStorageBytes = null;
        this.fileSize = null;
        this.communityId = null;
    }
    //#endregion

    //#region Getters
    public Long getCurrentStorageBytes() {
        return currentStorageBytes;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public Long getCommunityId() {
        return communityId;
    }

    public static long getMaxStorageBytes() {
        return MAX_STORAGE_BYTES;
    }

    /**
     * Retorna el almacenamiento disponible en bytes.
     */
    public Long getAvailableStorageBytes() {
        if (currentStorageBytes != null) {
            return MAX_STORAGE_BYTES - currentStorageBytes;
        }
        return null;
    }

    /**
     * Retorna el almacenamiento disponible en formato legible.
     */
    public String getAvailableStorageFormatted() {
        Long available = getAvailableStorageBytes();
        if (available != null) {
            return String.format("%.2f MB", available / (1024.0 * 1024.0));
        }
        return null;
    }
    //#endregion
}