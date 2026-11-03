package com.cvexpert.ofertas.adapter.out.messaging;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import com.cvexpert.ofertas.domain.model.ApplicationStatusChanged;
import com.cvexpert.ofertas.domain.model.NewJobMatched;
import com.cvexpert.ofertas.domain.port.out.EventPublisher;
import java.util.UUID;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.json.JsonMapper;

/**
 * Publica los eventos en la cola {@code notificaciones-eventos} de Azure Service Bus.
 *
 * <p>La cadena de conexión es la de solo envío ({@code ServiceBus--SendConnectionString} en el
 * Key Vault) y ya trae el nombre de la cola, así que aquí no se indica.
 *
 * <p>Si el envío falla, se registra y no se lanza: el cambio de estado ya quedó guardado, y
 * devolver un 500 haría creer que no. Es la limitación conocida de publicar después de guardar
 * (resolverla del todo exige el patrón outbox).
 */
public class ServiceBusEventPublisher implements EventPublisher, AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(ServiceBusEventPublisher.class);

    private final ServiceBusSenderClient sender;
    private final JsonMapper json;
    private final Supplier<UUID> eventIds;

    public ServiceBusEventPublisher(String connectionString, JsonMapper json) {
        this(new ServiceBusClientBuilder().connectionString(connectionString).sender().buildClient(), json,
                UUID::randomUUID);
        log.info("Los eventos se publican en Service Bus ({})", sender.getEntityPath());
    }

    ServiceBusEventPublisher(ServiceBusSenderClient sender, JsonMapper json, Supplier<UUID> eventIds) {
        this.sender = sender;
        this.json = json;
        this.eventIds = eventIds;
    }

    @Override
    public void publish(ApplicationStatusChanged event) {
        send(NotificationMessage.statusChanged(event, eventIds.get()));
    }

    @Override
    public void publish(NewJobMatched event) {
        send(NotificationMessage.newJob(event, eventIds.get()));
    }

    private void send(NotificationMessage message) {
        try {
            sender.sendMessage(toServiceBusMessage(message, json));
            log.info("Publicado {} {} para {}", message.type(), message.eventId(), message.userId());
        } catch (RuntimeException e) {
            log.error("No se pudo publicar {} {} para {}", message.type(), message.eventId(), message.userId(), e);
        }
    }

    /** El mensaje de Service Bus con las propiedades que pide Notificaciones. */
    static ServiceBusMessage toServiceBusMessage(NotificationMessage message, JsonMapper json) {
        ServiceBusMessage m = new ServiceBusMessage(json.writeValueAsString(message));
        m.setMessageId(message.eventId());
        m.setSubject(message.type());
        m.setContentType("application/json");
        return m;
    }

    @Override
    public void close() {
        sender.close();
    }
}
