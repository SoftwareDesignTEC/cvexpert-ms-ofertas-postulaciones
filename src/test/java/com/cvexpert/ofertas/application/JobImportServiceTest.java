package com.cvexpert.ofertas.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.cvexpert.ofertas.domain.model.ImportedJob;
import com.cvexpert.ofertas.domain.model.JobOfferDetails;
import com.cvexpert.ofertas.domain.model.JobSearchQuery;
import com.cvexpert.ofertas.domain.model.JobType;
import com.cvexpert.ofertas.domain.model.Modality;
import com.cvexpert.ofertas.domain.port.out.ExternalJobSource;
import com.cvexpert.ofertas.domain.port.out.ImportedJobRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class JobImportServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.ofEpochMilli(1_800_000_000_000L), ZoneOffset.UTC);
    private static final JobSearchQuery DEV = new JobSearchQuery("desarrollador", "Tecnología");
    private static final JobSearchQuery SUPPORT = new JobSearchQuery("soporte", "Tecnología");

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

    private static final class FakeRepository implements ImportedJobRepository {
        final Set<String> stored = new HashSet<>();
        final List<String> retired = new ArrayList<>();

        @Override
        public boolean upsert(ImportedJob job) {
            return stored.add(job.externalId());
        }

        @Override
        public int retireSource(String source, long deletedAt) {
            retired.add(source);
            return 10;
        }
    }

    private static ImportedJob job(String id) {
        var details = new JobOfferDetails("Dev " + id, "Empresa", null, "San José", Modality.ON_SITE,
                JobType.FULL_TIME, "Tecnología", "Descripción", List.of(), List.of("Java"), null, null,
                "https://empleos.example.com/" + id);
        return new ImportedJob("jsearch", id, details, 1L);
    }

    @Test
    void guardaLasOfertasYRetiraLasDeDemostracion() {
        var repo = new FakeRepository();
        repo.stored.add("b"); // ya importada antes: se actualiza
        var service = new JobImportService(new FakeSource(Map.of("desarrollador", List.of(job("a"), job("b")))),
                repo, List.of(DEV), CLOCK);

        var result = service.importJobs();

        assertThat(result.created()).isEqualTo(1);
        assertThat(result.updated()).isEqualTo(1);
        assertThat(result.retiredDemo()).isEqualTo(10);
        assertThat(repo.retired).containsExactly("demo");
    }

    @Test
    void unaBusquedaQueFallaNoFrenaLasDemas() {
        var repo = new FakeRepository();
        // "soporte" no está en el mapa: la fuente falla con esa búsqueda.
        var service = new JobImportService(new FakeSource(Map.of("desarrollador", List.of(job("a")))), repo,
                List.of(SUPPORT, DEV), CLOCK);

        var result = service.importJobs();

        assertThat(result.failedQueries()).isEqualTo(1);
        assertThat(result.created()).isEqualTo(1);
    }

    @Test
    void siNoLlegaNingunaOfertaNoRetiraLasDeDemostracion() {
        var repo = new FakeRepository();
        var service = new JobImportService(new FakeSource(Map.of()), repo, List.of(DEV), CLOCK);

        var result = service.importJobs();

        assertThat(result.retiredDemo()).isZero();
        assertThat(repo.retired).isEmpty();
    }
}
