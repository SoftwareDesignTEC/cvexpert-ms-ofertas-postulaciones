package com.cvexpert.ofertas.application;

import com.cvexpert.ofertas.domain.model.ImportedJob;
import com.cvexpert.ofertas.domain.model.JobSearchQuery;
import com.cvexpert.ofertas.domain.port.out.ExternalJobSource;
import com.cvexpert.ofertas.domain.port.out.ImportedJobRepository;
import java.time.Clock;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Trae ofertas reales de la fuente externa y las guarda.
 *
 * <p>Cada búsqueda configurada se pide por separado: si una falla (límite de la API, red), se
 * registra y se sigue con las demás. Las ofertas se guardan una por una por la misma razón: una
 * oferta rara no tumba la importación entera.
 *
 * <p>No publica el evento {@code job.new} de las alertas: una importación trae decenas de ofertas
 * de golpe y llenaría de notificaciones a los usuarios. Las alertas siguen saliendo para las
 * ofertas que crea un administrador.
 */
public class JobImportService {

    /** Fuente de las ofertas inventadas (ver V3__ofertas_reales.sql). */
    static final String DEMO_SOURCE = "demo";

    private static final Logger log = LoggerFactory.getLogger(JobImportService.class);

    private final ExternalJobSource source;
    private final ImportedJobRepository repository;
    private final List<JobSearchQuery> queries;
    private final Clock clock;

    public JobImportService(ExternalJobSource source, ImportedJobRepository repository,
            List<JobSearchQuery> queries, Clock clock) {
        this.source = source;
        this.repository = repository;
        this.queries = List.copyOf(queries);
        this.clock = clock;
    }

    /** Resultado de una corrida: cuántas ofertas se crearon, actualizaron y retiraron. */
    public record Result(int created, int updated, int failedQueries, int retiredDemo) {
    }

    public Result importJobs() {
        int created = 0;
        int updated = 0;
        int failedQueries = 0;
        for (JobSearchQuery query : queries) {
            List<ImportedJob> jobs;
            try {
                jobs = source.search(query);
            } catch (RuntimeException e) {
                failedQueries++;
                log.warn("La búsqueda \"{}\" falló en {}: {}", query.text(), source.name(), e.getMessage());
                continue;
            }
            for (ImportedJob job : jobs) {
                try {
                    if (repository.upsert(job)) {
                        created++;
                    } else {
                        updated++;
                    }
                } catch (RuntimeException e) {
                    log.warn("No se pudo guardar la oferta {} de {}: {}", job.externalId(), source.name(),
                            e.getMessage());
                }
            }
        }

        // Las ofertas inventadas se retiran solo cuando ya hay reales: si la API no respondió, la
        // app no se queda vacía.
        int retired = 0;
        if (created + updated > 0) {
            retired = repository.retireSource(DEMO_SOURCE, clock.millis());
        }

        Result result = new Result(created, updated, failedQueries, retired);
        log.info("Importación de {}: {} nuevas, {} actualizadas, {} búsquedas fallidas, {} de demostración retiradas",
                source.name(), created, updated, failedQueries, retired);
        return result;
    }
}
