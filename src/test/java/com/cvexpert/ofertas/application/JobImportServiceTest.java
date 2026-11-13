package com.cvexpert.ofertas.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.cvexpert.ofertas.domain.model.AlertSettings;
import com.cvexpert.ofertas.domain.model.CompatibilityCalculator;
import com.cvexpert.ofertas.domain.model.ImportedJob;
import com.cvexpert.ofertas.domain.model.JobOffer;
import com.cvexpert.ofertas.domain.model.JobOfferDetails;
import com.cvexpert.ofertas.domain.model.JobSearchQuery;
import com.cvexpert.ofertas.domain.model.JobType;
import com.cvexpert.ofertas.domain.model.MatchWeights;
import com.cvexpert.ofertas.domain.model.MatchingProfile;
import com.cvexpert.ofertas.domain.model.Modality;
import com.cvexpert.ofertas.domain.model.NewJobMatched;
import com.cvexpert.ofertas.domain.port.out.ExternalJobSource;
import com.cvexpert.ofertas.domain.port.out.ImportedJobRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JobImportServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.ofEpochMilli(1_800_000_000_000L), ZoneOffset.UTC);
    private static final JobSearchQuery DEV = new JobSearchQuery("desarrollador", "Tecnología");
    private static final JobSearchQuery SUPPORT = new JobSearchQuery("soporte", "Tecnología");
    private static final CompatibilityCalculator COMPATIBILITY =
            new CompatibilityCalculator(new MatchWeights(40, 20, 20, 20));

    /** Fuente de mentira: cada búsqueda devuelve lo que se le diga, o falla. */
    private static final class FakeSource implements ExternalJobSource {
        private final Map<String, List<ImportedJob>> results;

        FakeSource(Map<String, List<ImportedJob>> results) {
            this.results = results;
        }

        @Override
        public String name() {
            return "jsearch";
        }

        @Override
        public List<ImportedJob> search(JobSearchQuery query) {
            List<ImportedJob> jobs = results.get(query.text());
            if (jobs == null) {
                throw new IllegalStateException("429 Too Many Requests");
            }
            return jobs;
        }
    }

    /** Guarda en memoria y asigna ids, como la base. */
    private static final class FakeRepository implements ImportedJobRepository {
        final Map<String, Long> ids = new HashMap<>();
        final List<String> retired = new ArrayList<>();
        private long nextId = 1;

        @Override
        public Upsert upsert(ImportedJob job) {
            boolean created = !ids.containsKey(job.externalId());
            long id = ids.computeIfAbsent(job.externalId(), k -> nextId++);
            return new Upsert(new JobOffer(id, job.details(), job.postedAt()), created);
        }

        @Override
        public int retireSource(String source, long deletedAt) {
            retired.add(source);
            return 10;
        }
    }

    private FakeRepository repo;
    private InMemoryRepositories.Alerts alerts;
    private InMemoryRepositories.RecordingPublisher events;

    @BeforeEach
    void setUp() {
        repo = new FakeRepository();
        alerts = new InMemoryRepositories.Alerts();
        events = new InMemoryRepositories.RecordingPublisher();
    }

    private JobImportService service(Map<String, List<ImportedJob>> results, List<JobSearchQuery> queries,
            int maxAlerts) {
        return new JobImportService(new FakeSource(results), repo, queries, alerts, COMPATIBILITY, events, maxAlerts,
                CLOCK);
    }

    private static ImportedJob job(String id, String location, List<String> skills) {
        var details = new JobOfferDetails("Dev " + id, "Empresa", null, location, Modality.ON_SITE,
                JobType.FULL_TIME, "Tecnología", "Descripción", List.of(), skills, null, null,
                "https://empleos.example.com/" + id);
        return new ImportedJob("jsearch", id, details, 1L);
    }

    /** Un importador con una búsqueda que trae {@code count} ofertas (para JobRefreshServiceTest). */
    JobImportService importerReturning(int count) {
        setUp();
        List<ImportedJob> jobs = java.util.stream.IntStream.range(0, count).mapToObj(i -> job("r" + i)).toList();
        return service(Map.of(DEV.text(), jobs), List.of(DEV), 3);
    }

    private static ImportedJob job(String id) {
        return job(id, "San José", List.of("Java"));
    }

    /** Alguien de San José que sabe Java y quiere presencial a tiempo completo. */
    private void userWithAlerts(String userId, int minMatch) {
        var profile = new MatchingProfile(List.of("Java"), "San José", List.of(Modality.ON_SITE),
                List.of(JobType.FULL_TIME), List.of(), 2);
        alerts.save(new AlertSettings(userId, true, minMatch, profile, 1L));
    }

    @Test
    void guardaLasOfertasYRetiraLasDeDemostracion() {
        repo.ids.put("b", 99L); // ya importada antes: se actualiza
        var result = service(Map.of("desarrollador", List.of(job("a"), job("b"))), List.of(DEV), 3).importJobs();

        assertThat(result.created()).isEqualTo(1);
        assertThat(result.updated()).isEqualTo(1);
        assertThat(result.retiredDemo()).isEqualTo(10);
        assertThat(repo.retired).containsExactly("demo");
    }

    @Test
    void unaBusquedaQueFallaNoFrenaLasDemas() {
        // "soporte" no está en el mapa: la fuente falla con esa búsqueda.
        var result = service(Map.of("desarrollador", List.of(job("a"))), List.of(SUPPORT, DEV), 3).importJobs();

        assertThat(result.failedQueries()).isEqualTo(1);
        assertThat(result.created()).isEqualTo(1);
    }

    @Test
    void siNoLlegaNingunaOfertaNoRetiraLasDeDemostracion() {
        var result = service(Map.of(), List.of(DEV), 3).importJobs();

        assertThat(result.retiredDemo()).isZero();
        assertThat(repo.retired).isEmpty();
    }

    @Test
    void avisaDeLasOfertasNuevasQueAlcanzanElMinimo() {
        userWithAlerts("ana", 50);
        // "lejos" no comparte habilidades ni ubicación: no alcanza el mínimo.
        var result = service(Map.of("desarrollador",
                List.of(job("a"), job("lejos", "Limón", List.of("Contabilidad")))), List.of(DEV), 3).importJobs();

        assertThat(result.alertsSent()).isEqualTo(1);
        assertThat(events.newJobs).extracting(NewJobMatched::userId, NewJobMatched::title)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("ana", "Dev a"));
    }

    @Test
    void noAvisaDeLasQueSoloSeActualizaron() {
        userWithAlerts("ana", 0);
        repo.ids.put("a", 1L);

        var result = service(Map.of("desarrollador", List.of(job("a"))), List.of(DEV), 3).importJobs();

        assertThat(result.alertsSent()).isZero();
        assertThat(events.newJobs).isEmpty();
    }

    @Test
    void comoMuchoMaxAlertsPorUsuario() {
        userWithAlerts("ana", 0);
        var many = List.of(job("1"), job("2"), job("3"), job("4"), job("5"));

        var result = service(Map.of("desarrollador", many), List.of(DEV), 2).importJobs();

        assertThat(result.alertsSent()).isEqualTo(2);
        assertThat(events.newJobs).hasSize(2);
    }
}
