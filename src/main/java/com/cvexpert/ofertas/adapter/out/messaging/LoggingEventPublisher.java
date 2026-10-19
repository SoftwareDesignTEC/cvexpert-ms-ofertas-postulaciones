package com.cvexpert.ofertas.adapter.out.messaging;

import com.cvexpert.ofertas.domain.model.ApplicationStatusChanged;
import com.cvexpert.ofertas.domain.port.out.EventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stub del publicador: escribe el evento en el log en vez de mandarlo a Service Bus. Se usa hasta
 * que Samuel publique el esquema del mensaje y exista la cola {@code notificaciones-eventos}; ahí
 * se reemplaza por el adaptador real sin tocar el dominio.
 */
public class LoggingEventPublisher implements EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(LoggingEventPublisher.class);

    @Override
    public void publish(ApplicationStatusChanged event) {
        log.info("Evento application.status_changed (stub, no se envía a Service Bus): {}", event);
    }
}
