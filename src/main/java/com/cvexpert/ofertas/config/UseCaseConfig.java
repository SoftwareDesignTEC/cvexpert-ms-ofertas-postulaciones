package com.cvexpert.ofertas.config;

import com.cvexpert.ofertas.adapter.out.messaging.LoggingEventPublisher;
import com.cvexpert.ofertas.application.JobApplicationService;
import com.cvexpert.ofertas.application.JobOfferService;
import com.cvexpert.ofertas.domain.port.out.EventPublisher;
import com.cvexpert.ofertas.domain.port.out.JobApplicationRepository;
import com.cvexpert.ofertas.domain.port.out.JobOfferRepository;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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

    // TODO: cambiar por el adaptador de Service Bus cuando exista el esquema de Samuel.
    @Bean
    EventPublisher eventPublisher() {
        return new LoggingEventPublisher();
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
