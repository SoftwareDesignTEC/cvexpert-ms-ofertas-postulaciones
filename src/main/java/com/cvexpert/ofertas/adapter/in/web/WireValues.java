package com.cvexpert.ofertas.adapter.in.web;

import com.cvexpert.ofertas.domain.exception.InvalidRequestException;
import com.cvexpert.ofertas.domain.exception.NotFoundException;
import com.cvexpert.ofertas.domain.model.ApplicationStatus;
import java.util.Optional;

/**
 * Convierte los textos de la API a valores del dominio. Los ids viajan como texto ("123") pero en
 * la base son números.
 */
final class WireValues {

    private WireValues() {
    }

    /**
     * Id de una ruta ({@code /applications/{id}}). Si no es un número, la postulación no puede
     * existir, así que responde 404 igual que un id que no está.
     */
    static long applicationIdFromPath(String id) {
        return parseId(id).orElseThrow(() -> new NotFoundException("APPLICATION_NOT_FOUND", "No existe la postulación " + id));
    }

    /** Id que viene en el cuerpo. Si falta o no es un número, el cuerpo es inválido (400). */
    static long idFromBody(String id, String field) {
        return parseId(id).orElseThrow(() -> new InvalidRequestException(field + " tiene que ser un id válido"));
    }

    /** El estado del cuerpo de un cambio de estado. Un cuerpo vacío es un 400. */
    static ApplicationStatus status(ApplicationRequests.UpdateStatus body) {
        return ApplicationStatus.fromWireValue(body == null ? null : body.status())
                .orElseThrow(() -> new InvalidRequestException(
                        "status tiene que ser applied, in-progress, interview o rejected"));
    }

    private static Optional<Long> parseId(String id) {
        if (id == null || !id.matches("\\d{1,18}")) {
            return Optional.empty();
        }
        return Optional.of(Long.parseLong(id));
    }
}
