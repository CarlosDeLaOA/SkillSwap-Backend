package com.project.skillswap.logic.entity.auth;

import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servicio simple en memoria para invalidar tokens JWT hasta su expiración real.
 * - invalidateToken(token, exp) agrega el token a la lista negra hasta 'exp'.
 * - isInvalidated(token) retorna true si el token está en la lista negra y no ha pasado su TTL.
 * - Limpieza automática cada minuto.
 *

 */
@Service
@EnableScheduling
public class TokenBlacklistService {

    // token -> fecha/hora en que deja de considerarse inválido (suele ser la expiración del JWT)
    private final Map<String, Instant> blacklist = new ConcurrentHashMap<>();

    /**
     * Invalida el token hasta su expiración real.
          */
    public void invalidateToken(String token, Instant expiresAt) {
        Objects.requireNonNull(token, "token no puede ser null");
        if (expiresAt == null) {
            // fallback agresivo: invalídalo por 30 min si no hay exp (ajusta a tu necesidad)
            expiresAt = Instant.now().plusSeconds(30 * 60);
        }
        blacklist.put(token, expiresAt);
    }

    /** Devuelve true si el token fue invalidado y su TTL aún no pasó. */
    public boolean isInvalidated(String token) {
        Instant until = blacklist.get(token);
        if (until == null) return false;
        if (Instant.now().isAfter(until)) {
            // ya pasó el TTL: limpiamos
            blacklist.remove(token);
            return false;
        }
        return true;
    }

    /** Limpieza automática cada minuto. */
    @Scheduled(fixedDelay = 60_000)
    public void sweep() {
        Instant now = Instant.now();
        blacklist.entrySet().removeIf(e -> now.isAfter(e.getValue()));
    }
}
