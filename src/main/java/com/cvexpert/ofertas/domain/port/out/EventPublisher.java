package com.cvexpert.ofertas.domain.port.out;

import com.cvexpert.ofertas.domain.model.ApplicationStatusChanged;
import com.cvexpert.ofertas.domain.model.NewJobMatched;

/**
 * Publica los eventos que consume Notificaciones. El adaptador real escribe en la cola de Azure
 * Service Bus; en las pruebas se usa uno en memoria.
 */
public interface EventPublisher {

    void publish(ApplicationStatusChanged event);

    void publish(NewJobMatched event);
}
