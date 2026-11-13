package com.cvexpert.ofertas.adapter.in.web;

import com.cvexpert.ofertas.domain.exception.InvalidRequestException;
import com.cvexpert.ofertas.domain.exception.NotFoundException;
import com.cvexpert.ofertas.domain.model.ApplicationStatus;
import com.cvexpert.ofertas.domain.model.JobType;
import com.cvexpert.ofertas.domain.model.Modality;
import java.util.List;
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

    /** Id de oferta en la ruta ({@code /jobs/{id}}); si no es un número, 404. */
    static long jobIdFromPath(String id) {
        return parseId(id).orElseThrow(() -> new NotFoundException("JOB_NOT_FOUND", "No existe la oferta " + id));
    }

    static Modality modality(String value, String field) {
        return Modality.fromWireValue(value)
                .orElseThrow(() -> new InvalidRequestException(field + " tiene que ser remote, on-site o hybrid"));
    }

    static JobType jobType(String value, String field) {
        return JobType.fromWireValue(value).orElseThrow(() -> new InvalidRequestException(
                field + " tiene que ser full-time, part-time, freelance o internship"));
    }

    static List<Modality> modalities(List<String> values, String field) {
        return values == null ? null : values.stream().map(v -> modality(v, field)).toList();
    }

    static List<JobType> jobTypes(List<String> values, String field) {
        return values == null ? null : values.stream().map(v -> jobType(v, field)).toList();
    }

    /** Lanza un 400 si falta un campo obligatorio del cuerpo. */
    static <T> T required(T value, String field) {
        if (value == null) {
            throw new InvalidRequestException("Falta el campo " + field);
        }
        return value;
    }

    /** Id que viene en el cuerpo. Si falta o no es un número, el cuerpo es inválido (400). */
    static long idFromBody(String id, String field) {
        return parseId(id).orElseThrow(() -> new InvalidRequestException(field + " tiene que ser un id válido"));
    }

    /** El estado del cuerpo de un cambio de estado. Un cuerpo vacío es un 400. */
    static ApplicationStatus status(ApplicationRequests.UpdateStatus body) {
        return status(body == null ? null : body.status());
    }

    /** Un estado de la API; cualquier otro texto es un 400. */
    static ApplicationStatus status(String value) {
        return ApplicationStatus.fromWireValue(value)
                .orElseThrow(() -> new InvalidRequestException(
                        "status tiene que ser interested, applied, in-progress, interview o rejected"));
    }

    private static Optional<Long> parseId(String id) {
        if (id == null || !id.matches("\\d{1,18}")) {
            return Optional.empty();
        }
        return Optional.of(Long.parseLong(id));
    }
}
