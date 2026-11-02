package com.cvexpert.ofertas.adapter.out.messaging;

import com.cvexpert.ofertas.domain.model.ApplicationStatusChanged;
import com.cvexpert.ofertas.domain.model.NewJobMatched;
import java.util.UUID;

/**
 * Mensaje de la cola {@code notificaciones-eventos}, con el esquema que definió Notificaciones
 * (Samuel): un mensaje por destinatario, ids como texto y fechas en epoch milisegundos UTC. Si
 * falta un campo obligatorio, Notificaciones manda el mensaje a la cola de errores.
 *
 * @param eventId uuid v4; también va como {@code messageId} de Service Bus
 * @param userId  {@code sub} de Google de quien recibe el aviso
 * @param data    los datos propios de cada tipo de evento
 */
record NotificationMessage(
        int schemaVersion,
        String eventId,
        String type,
        String userId,
        long occurredAt,
        String source,
        Object data) {

    static final int SCHEMA_VERSION = 1;
    static final String SOURCE = "ofertas-postulaciones";
    static final String STATUS_CHANGED = "application.status_changed";
    static final String NEW_JOB = "job.new";

    static NotificationMessage statusChanged(ApplicationStatusChanged e, UUID eventId) {
        var data = new StatusChangedData(String.valueOf(e.applicationId()), String.valueOf(e.jobOfferId()),
                e.jobTitle(), e.company(), e.previousStatus().wireValue(), e.newStatus().wireValue());
        return new NotificationMessage(SCHEMA_VERSION, eventId.toString(), STATUS_CHANGED, e.userId(),
                e.occurredAt(), SOURCE, data);
    }

    static NotificationMessage newJob(NewJobMatched e, UUID eventId) {
        var data = new NewJobData(String.valueOf(e.jobOfferId()), e.title(), e.company(), e.location(),
                e.modality().wireValue(), e.matchScore());
        return new NotificationMessage(SCHEMA_VERSION, eventId.toString(), NEW_JOB, e.userId(), e.occurredAt(),
                SOURCE, data);
    }

    /** {@code data} de {@code job.new}. */
    record NewJobData(
            String jobOfferId,
            String title,
            String company,
            String location,
            String modality,
            int matchScore) {
    }

    /** {@code data} de {@code application.status_changed}. */
    record StatusChangedData(
            String applicationId,
            String jobOfferId,
            String jobTitle,
            String company,
            String previousStatus,
            String newStatus) {
    }
}
