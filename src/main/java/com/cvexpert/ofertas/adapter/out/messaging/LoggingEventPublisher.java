package com.cvexpert.ofertas.adapter.out.messaging;

import com.cvexpert.ofertas.domain.model.ApplicationStatusChanged;
import com.cvexpert.ofertas.domain.model.NewJobMatched;
import com.cvexpert.ofertas.domain.port.out.EventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stub del publicador: escribe el evento en el log en vez de mandarlo a Service Bus. Se usa cuando
 * no hay {@code SERVICEBUS_CONNECTION_STRING}: en local, en las pruebas y en Azure mientras la
 * variable no esté configurada.
 */
public class LoggingEventPublisher implements EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(LoggingEventPublisher.class);

    public LoggingEventPublisher() {
        log.warn("SERVICEBUS_CONNECTION_STRING no está definida: los eventos solo se escriben en el log");
    }

    @Override
    public void publish(ApplicationStatusChanged event) {
        log.info("Evento application.status_changed (stub, no se envía a Service Bus): {}", event);
    }

    @Override
    public void publish(NewJobMatched event) {
        log.info("Evento job.new (stub, no se envía a Service Bus): {}", event);
    }
}
