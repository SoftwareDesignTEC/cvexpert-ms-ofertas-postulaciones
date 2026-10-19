package com.cvexpert.ofertas.domain.port.out;

import com.cvexpert.ofertas.domain.model.ApplicationStatusChanged;

/**
 * Publica los eventos que consume Notificaciones. El adaptador real escribe en la cola de Azure
 * Service Bus; en las pruebas se usa uno en memoria.
 *
 * <p>El evento {@code job.new} se suma cuando exista el cálculo de compatibilidad.
 */
public interface EventPublisher {

    void publish(ApplicationStatusChanged event);
}
