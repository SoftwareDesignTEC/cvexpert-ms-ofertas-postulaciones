package com.cvexpert.ofertas.config;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import com.cvexpert.ofertas.adapter.out.messaging.LoggingEventPublisher;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class UseCaseConfigTest {

    private final UseCaseConfig config = new UseCaseConfig();
    private final JsonMapper json = JsonMapper.builder().build();

    @Test
    void withoutAConnectionStringEventsGoToTheLog() {
        assertInstanceOf(LoggingEventPublisher.class, config.eventPublisher("", json));
    }

    /** Lo que pasó el 2 de noviembre: una cadena inválida no puede impedir que la app arranque. */
    @Test
    void anInvalidConnectionStringDoesNotStopTheApp() {
        assertInstanceOf(LoggingEventPublisher.class, config.eventPublisher("estoNoEsUnaCadenaDeConexion=", json));
    }
}
