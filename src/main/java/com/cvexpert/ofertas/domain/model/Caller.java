package com.cvexpert.ofertas.domain.model;

import com.cvexpert.ofertas.domain.exception.ForbiddenException;
import com.cvexpert.ofertas.domain.exception.InvalidRequestException;

/**
 * Quien hace la llamada, tal como lo identifica APIM en los encabezados {@code X-User-Id},
 * {@code X-User-Email} y {@code X-User-Role}. El servicio confía en esos valores porque solo APIM
 * puede llegar a él (certificado de cliente).
 *
 * @param userId el {@code sub} de la cuenta de Google
 * @param role   {@code "admin"} en las rutas de administración; {@code null} en las demás
 */
public record Caller(String userId, String email, String role) {

    public static final String ADMIN_ROLE = "admin";

    public Caller {
        if (userId == null || userId.isBlank()) {
            throw new InvalidRequestException("Falta la identidad del usuario (X-User-Id)");
        }
    }

    public boolean isAdmin() {
        return ADMIN_ROLE.equals(role);
    }

    /** Corta la operación con {@link ForbiddenException} si quien llama no es administrador. */
    public void requireAdmin() {
        if (!isAdmin()) {
            throw new ForbiddenException();
        }
    }
}
