package com.cvexpert.ofertas.adapter.out.jobsource;

import static org.assertj.core.api.Assertions.assertThat;

import com.cvexpert.ofertas.domain.model.ImportedJob;
import com.cvexpert.ofertas.domain.model.JobSearchQuery;
import com.cvexpert.ofertas.domain.model.JobType;
import com.cvexpert.ofertas.domain.model.Modality;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/** Conversión de la respuesta de JSearch, con un JSON de ejemplo (sin llamar a la API). */
class JSearchJobSourceTest {

    private static final JobSearchQuery QUERY = new JobSearchQuery("desarrollador junior en Costa Rica", "Tecnología");
    private static final long NOW = 1_800_000_000_000L;

    private static final String RESPONSE = """
            {
              "status": "OK",
              "data": [
                {
                  "job_id": "abc123",
                  "job_title": "Desarrollador Java Junior",
                  "employer_name": "Empresa Real S.A.",
                  "employer_logo": "https://logo.example.com/empresa.png",
                  "job_employment_type": "FULLTIME",
                  "job_apply_link": "https://empleos.example.com/oferta/abc123",
                  "job_description": "Buscamos desarrollador con Java, Spring Boot y SQL. Inglés intermedio. Modalidad híbrida.",
                  "job_is_remote": false,
                  "job_posted_at_timestamp": 1799900000,
                  "job_city": "Heredia",
                  "job_state": "Heredia Province",
                  "job_min_salary": 900000,
                  "job_max_salary": 1200000,
                  "job_salary_currency": "CRC",
                  "job_required_experience": { "required_experience_in_months": 18 },
                  "job_highlights": { "Qualifications": ["Bachiller en computación", "Conocimiento de Git"] }
                },
                {
                  "job_id": "rem-1",
                  "job_title": "Customer Support Agent",
                  "employer_name": "Remote Co",
                  "job_employment_type": "PARTTIME",
                  "job_google_link": "https://www.google.com/search?q=job",
                  "job_description": "Customer service in English.",
                  "job_is_remote": true
                },
                { "job_id": "sin-empresa", "job_title": "Oferta incompleta" },
                {
                  "job_id": "salario-malo",
                  "job_title": "Salario al revés",
                  "employer_name": "Acme",
                  "job_min_salary": 500,
                  "job_max_salary": 100,
                  "job_salary_currency": "USD"
                }
              ]
            }
            """;

    private List<ImportedJob> parse() {
        return JSearchJobSource.toImportedJobs(JsonMapper.builder().build().readTree(RESPONSE), QUERY, NOW);
    }

    @Test
    void convierteUnaOfertaCompleta() {
        ImportedJob job = parse().get(0);

        assertThat(job.source()).isEqualTo("jsearch");
        assertThat(job.externalId()).isEqualTo("abc123");
        assertThat(job.postedAt()).isEqualTo(1_799_900_000_000L);
        var d = job.details();
        assertThat(d.title()).isEqualTo("Desarrollador Java Junior");
        assertThat(d.company()).isEqualTo("Empresa Real S.A.");
        assertThat(d.companyLogoUrl()).isEqualTo("https://logo.example.com/empresa.png");
        assertThat(d.location()).isEqualTo("Heredia");
        assertThat(d.modality()).isEqualTo(Modality.HYBRID);
        assertThat(d.type()).isEqualTo(JobType.FULL_TIME);
        assertThat(d.sector()).isEqualTo("Tecnología");
        assertThat(d.requirements()).containsExactly("Bachiller en computación", "Conocimiento de Git");
        assertThat(d.skills()).contains("Java", "Spring Boot", "SQL", "Inglés").doesNotContain("JavaScript");
        assertThat(d.minExperienceYears()).isEqualTo(1);
        assertThat(d.salary().min()).isEqualByComparingTo(BigDecimal.valueOf(900000));
        assertThat(d.salary().currency()).isEqualTo("CRC");
        assertThat(d.applyUrl()).isEqualTo("https://empleos.example.com/oferta/abc123");
    }

    @Test
    void ofertaRemotaSinCiudadNiEnlaceDePostulacion() {
        var d = parse().get(1).details();

        assertThat(d.modality()).isEqualTo(Modality.REMOTE);
        assertThat(d.location()).isEqualTo("Todo el país");
        assertThat(d.type()).isEqualTo(JobType.PART_TIME);
        assertThat(d.salary()).isNull();
        // Sin job_apply_link se usa el enlace de Google for Jobs.
        assertThat(d.applyUrl()).isEqualTo("https://www.google.com/search?q=job");
        assertThat(parse().get(1).postedAt()).isEqualTo(NOW);
    }

    @Test
    void descartaLasIncompletasYLasQueNoCumplenLasReglas() {
        assertThat(parse()).extracting(ImportedJob::externalId).containsExactly("abc123", "rem-1");
    }
}
