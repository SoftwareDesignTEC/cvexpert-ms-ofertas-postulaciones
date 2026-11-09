package com.cvexpert.ofertas.application;

import com.cvexpert.ofertas.domain.model.AlertSettings;
import com.cvexpert.ofertas.domain.model.CompatibilityCalculator;
import com.cvexpert.ofertas.domain.model.ImportedJob;
import com.cvexpert.ofertas.domain.model.JobOffer;
import com.cvexpert.ofertas.domain.model.JobSearchQuery;
import com.cvexpert.ofertas.domain.model.NewJobMatched;
import com.cvexpert.ofertas.domain.port.out.AlertSettingsRepository;
import com.cvexpert.ofertas.domain.port.out.EventPublisher;
import com.cvexpert.ofertas.domain.port.out.ExternalJobSource;
import com.cvexpert.ofertas.domain.port.out.ImportedJobRepository;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Trae ofertas reales de la fuente externa, las guarda y avisa a quien le interesan.
 *
 * <p>Cada búsqueda configurada se pide por separado: si una falla (límite de la API, red), se
 * registra y se sigue con las demás. Las ofertas se guardan una por una por la misma razón: una
 * oferta rara no tumba la importación entera.
 *
 * <p>Alertas ({@code job.new}): al final de la corrida, para cada usuario con alertas activas se
 * calcula la compatibilidad con las ofertas <b>nuevas</b> (las actualizadas ya se avisaron cuando
 * entraron) y se avisa de las que alcanzan su mínimo. Como una importación puede traer decenas de
 * ofertas de golpe, se avisa como mucho de las {@code maxAlertsPerUser} más compatibles, para no
 * llenarle la bandeja a nadie.
 */
public class JobImportService {

    /** Fuente de las ofertas inventadas (ver V3__ofertas_reales.sql). */
    static final String DEMO_SOURCE = "demo";

    private static final Logger log = LoggerFactory.getLogger(JobImportService.class);

    private final ExternalJobSource source;
    private final ImportedJobRepository repository;
    private final List<JobSearchQuery> queries;
    private final AlertSettingsRepository alerts;
    private final CompatibilityCalculator compatibility;
    private final EventPublisher events;
    private final int maxAlertsPerUser;
    private final Clock clock;

    public JobImportService(ExternalJobSource source, ImportedJobRepository repository, List<JobSearchQuery> queries,
            AlertSettingsRepository alerts, CompatibilityCalculator compatibility, EventPublisher events,
            int maxAlertsPerUser, Clock clock) {
        this.source = source;
        this.repository = repository;
        this.queries = List.copyOf(queries);
        this.alerts = alerts;
        this.compatibility = compatibility;
        this.events = events;
        this.maxAlertsPerUser = maxAlertsPerUser;
        this.clock = clock;
    }

    /** Resultado de una corrida: ofertas creadas, actualizadas, búsquedas fallidas, retiradas y avisos. */
    public record Result(int created, int updated, int failedQueries, int retiredDemo, int alertsSent) {
    }

    public Result importJobs() {
        List<JobOffer> createdOffers = new ArrayList<>();
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
                    ImportedJobRepository.Upsert saved = repository.upsert(job);
                    if (saved.created()) {
                        createdOffers.add(saved.offer());
                    } else {
                        updated++;
                    }
                } catch (RuntimeException e) {
                    log.warn("No se pudo guardar la oferta \"{}\" de {}: {}", job.details().title(), source.name(),
                            e.getMessage());
                }
            }
        }

        // Las ofertas inventadas se retiran solo cuando ya hay reales: si la API no respondió, la
        // app no se queda vacía.
        int retired = 0;
        if (!createdOffers.isEmpty() || updated > 0) {
            retired = repository.retireSource(DEMO_SOURCE, clock.millis());
        }

        int alertsSent = notifyMatchingUsers(createdOffers);

        Result result = new Result(createdOffers.size(), updated, failedQueries, retired, alertsSent);
        log.info("Importación de {}: {} nuevas, {} actualizadas, {} búsquedas fallidas, {} de demostración "
                + "retiradas, {} avisos job.new", source.name(), result.created(), updated, failedQueries, retired,
                alertsSent);
        return result;
    }

    /**
     * Un {@code job.new} por cada oferta nueva que alcanza el mínimo del usuario, como mucho
     * {@code maxAlertsPerUser} por usuario y empezando por la más compatible.
     */
    private int notifyMatchingUsers(List<JobOffer> createdOffers) {
        if (createdOffers.isEmpty()) {
            return 0;
        }
        int sent = 0;
        for (AlertSettings settings : alerts.findEnabled()) {
            List<NewJobMatched> matches = createdOffers.stream()
                    .map(offer -> NewJobMatched.of(settings.userId(), offer,
                            compatibility.score(offer, settings.profile())))
                    .filter(event -> settings.wantsAlertFor(event.matchScore()))
                    .sorted(Comparator.comparingInt(NewJobMatched::matchScore).reversed())
                    .limit(maxAlertsPerUser)
                    .toList();
            for (NewJobMatched event : matches) {
                try {
                    events.publish(event);
                    sent++;
                } catch (RuntimeException e) {
                    // Un aviso que no sale no deshace la importación: las ofertas ya están guardadas.
                    log.warn("No se pudo publicar job.new para la oferta {}: {}", event.jobOfferId(),
                            e.getMessage());
                }
            }
        }
        return sent;
    }
}
