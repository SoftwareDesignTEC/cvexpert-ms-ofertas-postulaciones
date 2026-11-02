package com.cvexpert.ofertas.domain.model;

import com.cvexpert.ofertas.domain.exception.InvalidRequestException;

/**
 * Configuración de alertas de un usuario: si quiere avisos de ofertas nuevas, desde qué
 * compatibilidad, y el perfil con que se calcula.
 *
 * @param minMatch  compatibilidad mínima (0 a 100) para recibir el aviso
 * @param updatedAt epoch milisegundos UTC del último guardado
 */
public record AlertSettings(String userId, boolean enabled, int minMatch, MatchingProfile profile, long updatedAt) {

    public AlertSettings {
        if (minMatch < 0 || minMatch > 100) {
            throw new InvalidRequestException("minMatch tiene que estar entre 0 y 100");
        }
        if (profile == null) {
            throw new InvalidRequestException("Falta el campo profile");
        }
    }

    /** Si una oferta con esta compatibilidad le genera aviso. */
    public boolean wantsAlertFor(int matchScore) {
        return enabled && matchScore >= minMatch;
    }
}
