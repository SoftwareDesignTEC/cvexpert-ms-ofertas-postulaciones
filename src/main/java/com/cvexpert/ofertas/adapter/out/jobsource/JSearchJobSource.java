package com.cvexpert.ofertas.adapter.out.jobsource;

import com.cvexpert.ofertas.domain.model.ImportedJob;
import com.cvexpert.ofertas.domain.model.JobOfferDetails;
import com.cvexpert.ofertas.domain.model.JobSearchQuery;
import com.cvexpert.ofertas.domain.model.JobType;
import com.cvexpert.ofertas.domain.model.Modality;
import com.cvexpert.ofertas.domain.model.Salary;
import com.cvexpert.ofertas.domain.model.SkillExtractor;
import com.cvexpert.ofertas.domain.port.out.ExternalJobSource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Ofertas reales de JSearch, una API que junta las ofertas publicadas en LinkedIn, Indeed,
 * Glassdoor y otros sitios a través de Google for Jobs.
 *
 * <p>Se usa {@code GET /search} con la búsqueda en texto ("desarrollador junior en Costa Rica"), el
 * país y el rango de fechas. La clave viaja en un encabezado; con RapidAPI son
 * {@code X-RapidAPI-Key} y {@code X-RapidAPI-Host}, y con OpenWeb Ninja directo, {@code x-api-key}.
 * Por eso los nombres y la URL son configurables (ver {@code application.yml}, sección importer).
 *
 * <p>La conversión del JSON está en {@link #toImportedJobs}, separada de la llamada HTTP, para
 * probarla con una respuesta de ejemplo sin gastar consultas de la API.
 */
public class JSearchJobSource implements ExternalJobSource {

    public static final String SOURCE = "jsearch";

    private static final Logger log = LoggerFactory.getLogger(JSearchJobSource.class);

    /** La descripción completa puede ser muy larga; la tarjeta y el detalle no necesitan más. */
    private static final int MAX_DESCRIPTION = 4000;
    private static final int MAX_REQUIREMENTS = 6;

    private final RestClient http;
    private final JsonMapper json;
    private final String country;
    private final String datePosted;
    private final java.time.Clock clock;

    public JSearchJobSource(RestClient http, JsonMapper json, String country, String datePosted,
            java.time.Clock clock) {
        this.http = http;
        this.json = json;
        this.country = country;
        this.datePosted = datePosted;
        this.clock = clock;
    }

    @Override
    public String name() {
        return SOURCE;
    }

    @Override
    public List<ImportedJob> search(JobSearchQuery query) {
        String body = http.get()
                .uri(uri -> uri.path("/search")
                        .queryParam("query", query.text())
                        .queryParam("page", 1)
                        .queryParam("num_pages", 1)
                        .queryParam("country", country)
                        .queryParam("date_posted", datePosted)
                        .build())
                .retrieve()
                .body(String.class);
        return toImportedJobs(json.readTree(body == null ? "{}" : body), query, clock.millis());
    }

    /** Convierte la respuesta de {@code /search}. Descarta las ofertas que no se pueden usar. */
    static List<ImportedJob> toImportedJobs(JsonNode root, JobSearchQuery query, long now) {
        List<ImportedJob> jobs = new ArrayList<>();
        for (JsonNode node : root.path("data")) {
            try {
                ImportedJob job = toImportedJob(node, query, now);
                if (job != null) {
                    jobs.add(job);
                }
            } catch (RuntimeException e) {
                // Una oferta que no cumple las reglas del dominio (por ejemplo, un salario con el
                // mínimo mayor que el máximo) se salta; las demás se importan igual.
                log.debug("Se descartó la oferta {} de JSearch: {}", text(node, "job_id"), e.getMessage());
            }
        }
        return jobs;
    }

    private static ImportedJob toImportedJob(JsonNode node, JobSearchQuery query, long now) {
        String id = text(node, "job_id");
        String title = text(node, "job_title");
        String company = text(node, "employer_name");
        if (id == null || title == null || company == null) {
            return null;
        }
        String description = text(node, "job_description");
        if (description == null) {
            description = title;
        } else if (description.length() > MAX_DESCRIPTION) {
            description = description.substring(0, MAX_DESCRIPTION).trim() + "…";
        }

        JobOfferDetails details = new JobOfferDetails(
                title,
                company,
                text(node, "employer_logo"),
                location(node),
                modality(node, description),
                jobType(node),
                query.sector(),
                description,
                requirements(node),
                skills(node, title + " " + description),
                experienceYears(node),
                salary(node),
                applyUrl(node));

        long postedAt = node.path("job_posted_at_timestamp").isNumber()
                ? node.path("job_posted_at_timestamp").asLong() * 1000
                : now;
        return new ImportedJob(SOURCE, id, details, postedAt);
    }

    /** Ciudad, si no la provincia, y si no "Costa Rica". Las remotas sin ciudad: "Todo el país". */
    private static String location(JsonNode node) {
        String city = text(node, "job_city");
        if (city != null) {
            return city;
        }
        String state = text(node, "job_state");
        if (state != null) {
            return state;
        }
        return node.path("job_is_remote").asBoolean(false) ? "Todo el país" : "Costa Rica";
    }

    /** JSearch solo dice si es remota; "híbrido" se reconoce en la descripción. */
    private static Modality modality(JsonNode node, String description) {
        if (node.path("job_is_remote").asBoolean(false)) {
            return Modality.REMOTE;
        }
        String lower = description.toLowerCase(Locale.ROOT);
        if (lower.contains("híbrid") || lower.contains("hibrid") || lower.contains("hybrid")) {
            return Modality.HYBRID;
        }
        return Modality.ON_SITE;
    }

    private static JobType jobType(JsonNode node) {
        String type = text(node, "job_employment_type");
        if (type == null) {
            return JobType.FULL_TIME;
        }
        String upper = type.toUpperCase(Locale.ROOT).replace(" ", "").replace("-", "");
        if (upper.contains("INTERN")) {
            return JobType.INTERNSHIP;
        }
        if (upper.contains("PARTTIME")) {
            return JobType.PART_TIME;
        }
        if (upper.contains("CONTRACTOR") || upper.contains("FREELANCE")) {
            return JobType.FREELANCE;
        }
        return JobType.FULL_TIME;
    }

    /** Los requisitos que JSearch separa en job_highlights.Qualifications. */
    private static List<String> requirements(JsonNode node) {
        List<String> list = new ArrayList<>();
        for (JsonNode item : node.path("job_highlights").path("Qualifications")) {
            String value = item.isString() ? item.asString().trim() : "";
            if (!value.isEmpty()) {
                list.add(value);
            }
            if (list.size() == MAX_REQUIREMENTS) {
                break;
            }
        }
        return list;
    }

    /** Las que manda JSearch, si las manda; si no, las que se reconocen en el texto. */
    private static List<String> skills(JsonNode node, String text) {
        List<String> list = new ArrayList<>();
        for (JsonNode item : node.path("job_required_skills")) {
            if (item.isString() && !item.asString().isBlank()) {
                list.add(item.asString().trim());
            }
        }
        return list.isEmpty() ? SkillExtractor.extract(text) : list;
    }

    private static Integer experienceYears(JsonNode node) {
        JsonNode months = node.path("job_required_experience").path("required_experience_in_months");
        return months.isNumber() ? Math.max(0, months.asInt() / 12) : null;
    }

    /** Solo si vienen mínimo, máximo y moneda; si falta algo, la oferta va sin salario. */
    private static Salary salary(JsonNode node) {
        JsonNode min = node.path("job_min_salary");
        JsonNode max = node.path("job_max_salary");
        String currency = text(node, "job_salary_currency");
        if (!min.isNumber() || !max.isNumber() || currency == null) {
            return null;
        }
        return new Salary(BigDecimal.valueOf(min.asDouble()), BigDecimal.valueOf(max.asDouble()), currency);
    }

    /** El enlace de postulación; si no hay, el de Google for Jobs, que también lleva a la oferta. */
    private static String applyUrl(JsonNode node) {
        String apply = text(node, "job_apply_link");
        return apply != null ? apply : text(node, "job_google_link");
    }

    /** Texto del campo, o {@code null} si falta, es null o está vacío. */
    private static String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (!value.isString()) {
            return null;
        }
        String s = value.asString().trim();
        return s.isEmpty() ? null : s;
    }
}
