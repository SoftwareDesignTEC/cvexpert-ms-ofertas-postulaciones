package com.cvexpert.ofertas.config;

import com.cvexpert.ofertas.adapter.out.jobsource.JSearchJobSource;
import com.cvexpert.ofertas.application.JobImportService;
import com.cvexpert.ofertas.domain.model.JobSearchQuery;
import com.cvexpert.ofertas.domain.port.out.ImportedJobRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

/**
 * Importación de ofertas reales: arma el importador y lo corre cada dos días (y una vez al
 * arrancar).
 *
 * <p>Si no hay clave de la API ({@code JSEARCH_API_KEY}), no se importa nada y el servicio sigue
 * con las ofertas que tenga. Así las pruebas y el desarrollo local no necesitan la API.
 */
@Configuration
@EnableScheduling
@EnableConfigurationProperties(JobImportConfig.ImporterProperties.class)
public class JobImportConfig {

    private static final Logger log = LoggerFactory.getLogger(JobImportConfig.class);

    /** Espera antes de la importación inicial, para no competir con el arranque. */
    private static final Duration STARTUP_DELAY = Duration.ofSeconds(20);

    private final ImporterProperties properties;
    private final JobImportService importer;
    private final TaskScheduler scheduler;

    /**
     * Sección {@code importer} de {@code application.yml}.
     *
     * @param queries búsquedas que se hacen en cada corrida. El plan gratis de JSearch da 200
     *                consultas al mes: con 6 búsquedas cada dos días son unas 90.
     */
    @ConfigurationProperties(prefix = "importer")
    public record ImporterProperties(boolean runOnStartup, JSearch jsearch, List<JobSearchQuery> queries) {

        public ImporterProperties {
            queries = queries == null ? List.of() : List.copyOf(queries);
        }

        /** Conexión con JSearch. Ver JSearchJobSource para los encabezados. */
        public record JSearch(String apiKey, String baseUrl, String keyHeader, String hostHeader, String country,
                String datePosted) {
        }

        boolean configured() {
            return jsearch != null && jsearch.apiKey() != null && !jsearch.apiKey().isBlank();
        }
    }

    JobImportConfig(ImporterProperties properties, ImportedJobRepository repository, JsonMapper json, Clock clock,
            TaskScheduler scheduler) {
        this.properties = properties;
        this.scheduler = scheduler;
        this.importer = properties.configured()
                ? new JobImportService(jsearch(properties.jsearch(), json, clock), repository, properties.queries(),
                        clock)
                : null;
    }

    private static JSearchJobSource jsearch(ImporterProperties.JSearch config, JsonMapper json, Clock clock) {
        // La API a veces tarda; sin límite, un cuelgue dejaría el hilo del importador trabado
        // hasta la próxima corrida.
        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(10));
        // 60 s: la v5 a veces tarda más de 30 en responder una búsqueda.
        requestFactory.setReadTimeout(Duration.ofSeconds(60));
        RestClient.Builder builder = RestClient.builder()
                .baseUrl(config.baseUrl())
                .requestFactory(requestFactory)
                .defaultHeader(config.keyHeader(), config.apiKey());
        if (config.hostHeader() != null && !config.hostHeader().isBlank()) {
            builder.defaultHeader("X-RapidAPI-Host", config.hostHeader());
        }
        return new JSearchJobSource(builder.build(), json, config.country(), config.datePosted(), clock);
    }

    /** Una vez al arrancar, un rato después, para que haya ofertas reales apenas se despliega. */
    @EventListener(ApplicationReadyEvent.class)
    void importOnStartup() {
        if (importer == null) {
            log.info("Importación de ofertas apagada: falta JSEARCH_API_KEY");
            return;
        }
        if (properties.runOnStartup()) {
            log.info("Importación inicial de ofertas programada en {} s", STARTUP_DELAY.toSeconds());
            scheduler.schedule(this::runImport, Instant.now().plus(STARTUP_DELAY));
        }
    }

    /** A la hora de {@code importer.cron} (cada dos días, hora de Costa Rica). */
    @Scheduled(cron = "${importer.cron}", zone = "America/Costa_Rica")
    void dailyImport() {
        if (importer != null) {
            runImport();
        }
    }

    private void runImport() {
        try {
            importer.importJobs();
        } catch (RuntimeException e) {
            // Nunca se deja escapar: el programador dejaría de correr la tarea.
            log.error("La importación de ofertas falló", e);
        }
    }
}
