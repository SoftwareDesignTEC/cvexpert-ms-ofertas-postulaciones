package com.cvexpert.ofertas.config;

import com.cvexpert.ofertas.adapter.out.messaging.LoggingEventPublisher;
import com.cvexpert.ofertas.adapter.out.messaging.ServiceBusEventPublisher;
import com.cvexpert.ofertas.application.JobApplicationService;
import com.cvexpert.ofertas.application.JobOfferService;
import com.cvexpert.ofertas.domain.port.out.EventPublisher;
import com.cvexpert.ofertas.domain.port.out.JobApplicationRepository;
import com.cvexpert.ofertas.domain.port.out.JobOfferRepository;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

/**
 * Arma los casos de uso. Los servicios no llevan anotaciones de Spring: se crean aquí y reciben los
 * adaptadores por sus puertos. Los repositorios los encuentra Spring porque los adaptadores de
 * persistencia son {@code @Component}.
 */
@Configuration
public class UseCaseConfig {

    /** Reloj en UTC. Las pruebas usan uno fijo para controlar las fechas. */
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    /**
     * Service Bus si hay cadena de conexión ({@code SERVICEBUS_CONNECTION_STRING}); si no, el stub
     * que escribe en el log. Así las pruebas y el desarrollo local no necesitan Azure. Spring
     * cierra la conexión con Service Bus al apagar la app.
     */
    @Bean
    EventPublisher eventPublisher(@Value("${servicebus.connection-string:}") String connectionString,
            JsonMapper json) {
        if (connectionString.isBlank()) {
            return new LoggingEventPublisher();
        }
        return new ServiceBusEventPublisher(connectionString, json);
    }

    @Bean
    JobOfferService jobOfferService(JobOfferRepository offers, Clock clock) {
        return new JobOfferService(offers, clock);
    }

    @Bean
    JobApplicationService jobApplicationService(JobApplicationRepository applications, JobOfferRepository offers,
            EventPublisher events, Clock clock) {
        return new JobApplicationService(applications, offers, events, clock);
    }
}
