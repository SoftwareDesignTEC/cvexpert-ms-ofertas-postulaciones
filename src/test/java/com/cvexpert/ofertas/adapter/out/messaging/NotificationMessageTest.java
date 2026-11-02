package com.cvexpert.ofertas.adapter.out.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.azure.messaging.servicebus.ServiceBusMessage;
import com.cvexpert.ofertas.domain.model.ApplicationStatus;
import com.cvexpert.ofertas.domain.model.ApplicationStatusChanged;
import com.cvexpert.ofertas.domain.model.Modality;
import com.cvexpert.ofertas.domain.model.NewJobMatched;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Comprueba que el mensaje cumple al pie de la letra el esquema de Notificaciones. Si un campo
 * obligatorio falta o cambia de tipo, Notificaciones lo manda a la cola de errores y nadie se
 * entera, así que esta prueba es la que avisa.
 */
class NotificationMessageTest {

    private static final UUID EVENT_ID = UUID.fromString("3f2b8c1e-9d4a-4f6b-8e2a-1c5d7e9f0a3b");

    private final JsonMapper json = JsonMapper.builder().build();

    private final ApplicationStatusChanged event = new ApplicationStatusChanged(123L, "google-sub-ana", 45L,
            "Desarrollador Android", "Acme CR", ApplicationStatus.APPLIED, ApplicationStatus.INTERVIEW,
            1_792_108_800_000L);

    @Test
    void bodyFollowsTheNotificationsSchema() {
        ServiceBusMessage message = ServiceBusEventPublisher.toServiceBusMessage(
                NotificationMessage.statusChanged(event, EVENT_ID), json);

        JsonNode body = json.readTree(message.getBody().toString());

        assertEquals(1, body.get("schemaVersion").asInt());
        assertEquals(EVENT_ID.toString(), body.get("eventId").asString());
        assertEquals("application.status_changed", body.get("type").asString());
        assertEquals("google-sub-ana", body.get("userId").asString());
        assertTrue(body.get("occurredAt").isIntegralNumber(), "occurredAt va como número (epoch ms)");
        assertEquals(1_792_108_800_000L, body.get("occurredAt").asLong());
        assertEquals("ofertas-postulaciones", body.get("source").asString());

        JsonNode data = body.get("data");
        assertTrue(data.get("applicationId").isString(), "los ids dentro de data van como texto");
        assertEquals("123", data.get("applicationId").asString());
        assertEquals("45", data.get("jobOfferId").asString());
        assertEquals("Desarrollador Android", data.get("jobTitle").asString());
        assertEquals("Acme CR", data.get("company").asString());
        assertEquals("applied", data.get("previousStatus").asString());
        assertEquals("interview", data.get("newStatus").asString());
    }

    @Test
    void serviceBusPropertiesMatchTheSchema() {
        ServiceBusMessage message = ServiceBusEventPublisher.toServiceBusMessage(
                NotificationMessage.statusChanged(event, EVENT_ID), json);

        assertEquals(EVENT_ID.toString(), message.getMessageId());
        assertEquals("application.status_changed", message.getSubject());
        assertEquals("application/json", message.getContentType());
    }

    @Test
    void newJobFollowsTheNotificationsSchema() {
        NewJobMatched newJob = new NewJobMatched("google-sub-luis", 45L, "Desarrollador Android", "Acme CR",
                "San José", Modality.ON_SITE, 85, 1_792_108_800_000L);
        ServiceBusMessage message = ServiceBusEventPublisher.toServiceBusMessage(
                NotificationMessage.newJob(newJob, EVENT_ID), json);

        JsonNode body = json.readTree(message.getBody().toString());
        assertEquals("job.new", body.get("type").asString());
        assertEquals("job.new", message.getSubject());
        assertEquals("google-sub-luis", body.get("userId").asString());
        assertEquals("ofertas-postulaciones", body.get("source").asString());

        JsonNode data = body.get("data");
        assertEquals("45", data.get("jobOfferId").asString());
        assertEquals("Desarrollador Android", data.get("title").asString());
        assertEquals("Acme CR", data.get("company").asString());
        assertEquals("San José", data.get("location").asString());
        assertEquals("on-site", data.get("modality").asString());
        assertTrue(data.get("matchScore").isIntegralNumber());
        assertEquals(85, data.get("matchScore").asInt());
    }
}
