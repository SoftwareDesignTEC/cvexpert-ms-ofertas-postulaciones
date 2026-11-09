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
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Ofertas reales de JSearch, una API que junta las ofertas publicadas en LinkedIn, Indeed,
 * Glassdoor y otros sitios a través de Google for Jobs.
 *
 * <p>Se usa {@code GET /search-v2} (la versión 5 de la API) con la búsqueda en texto
 * ("desarrollador junior en Costa Rica"), el país, el idioma y el rango de fechas. La v5 devuelve
 * las ofertas en {@code data.jobs}; las versiones anteriores, directo en {@code data}. Se aceptan
 * las dos formas. La clave viaja en un encabezado; con RapidAPI son
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

    /** "San José · a través de LinkedIn": lo que va antes de "a través de" es la ubicación. */
    private static final Pattern VIA = Pattern.compile("(?i)\\s*(a trav[eé]s de|via)\\s.*$");

    /** "(y 1 ubicación más)" o "(y 3 ubicaciones más)", con o sin el paréntesis de cierre. */
    private static final Pattern MORE_PLACES = Pattern.compile("(?i)\\(\\s*y\\s+\\d+\\s+ubicaci\\S*\\s+m[aá]s\\s*\\)?");

    /** "Provincia de Alajuela" → "Alajuela". */
    private static final Pattern PROVINCE = Pattern.compile("(?i)provincia de\\s+");

    /** Lo que sobra al recortar: espacios, viñetas y separadores. */
    private static final Pattern EDGES = Pattern.compile("^[\\s\\p{Punct}·•]+|[\\s\\p{Punct}·•]+$");

    /** "hace 7 días", "hace 3 horas": la v5 a veces solo trae la fecha así. */
    private static final Pattern AGO = Pattern.compile("(?i)hace\\s+(\\d+)\\s+(minuto|hora|d[ií]a|semana|mes)");

    private static final long MINUTE = 60_000L;
    private static final long HOUR = 60 * MINUTE;
    private static final long DAY = 24 * HOUR;

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
        // num_pages=1: cada página (hasta 10 ofertas) gasta una consulta del plan.
        String body = http.get()
                .uri(uri -> uri.path("/search-v2")
                        .queryParam("query", query.text())
                        .queryParam("num_pages", 1)
                        .queryParam("country", country)
                        .queryParam("language", "es")
                        .queryParam("date_posted", datePosted)
                        .build())
                .retrieve()
                .body(String.class);
        return toImportedJobs(json.readTree(body == null ? "{}" : body), query, clock.millis());
    }

    /** Convierte la respuesta de {@code /search}. Descarta las ofertas que no se pueden usar. */
    static List<ImportedJob> toImportedJobs(JsonNode root, JobSearchQuery query, long now) {
        List<ImportedJob> jobs = new ArrayList<>();
        JsonNode data = root.path("data");
        JsonNode list = data.isArray() ? data : data.path("jobs");
        for (JsonNode node : list) {
            try {
                ImportedJob job = toImportedJob(node, query, now);
                if (job != null) {
                    jobs.add(job);
                }
            } catch (RuntimeException e) {
                // Una oferta que no cumple las reglas del dominio (por ejemplo, un salario con el
                // mínimo mayor que el máximo) se salta; las demás se importan igual.
                log.debug("Se descartó la oferta \"{}\" de JSearch: {}", text(node, "job_title"), e.getMessage());
            }
        }
        return jobs;
    }

    private static ImportedJob toImportedJob(JsonNode node, JobSearchQuery query, long now) {
        String title = text(node, "job_title");
        String company = text(node, "employer_name");
        if (title == null || company == null) {
            return null;
        }
        String id = stableId(title, company);
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

        return new ImportedJob(SOURCE, id, details, postedAt(node, now));
    }

    /**
     * Fecha de publicación: el timestamp si viene; si no, la fecha ISO; si no, el texto
     * "hace 7 días"; y si no hay nada, ahora.
     */
    private static long postedAt(JsonNode node, long now) {
        JsonNode timestamp = node.path("job_posted_at_timestamp");
        if (timestamp.isNumber()) {
            return timestamp.asLong() * 1000;
        }
        String iso = text(node, "job_posted_at_datetime_utc");
        if (iso != null) {
            try {
                return Instant.parse(iso).toEpochMilli();
            } catch (DateTimeParseException ignored) {
                // Se sigue con el texto.
            }
        }
        String ago = text(node, "job_posted_at");
        if (ago != null) {
            Matcher m = AGO.matcher(ago);
            if (m.find()) {
                long amount = Long.parseLong(m.group(1));
                String unit = m.group(2).toLowerCase(Locale.ROOT);
                long unitMillis = switch (unit.charAt(0)) {
                    case 'm' -> unit.startsWith("mi") ? MINUTE : 30 * DAY;
                    case 'h' -> HOUR;
                    case 's' -> 7 * DAY;
                    default -> DAY;
                };
                return now - amount * unitMillis;
            }
        }
        return now;
    }

    /**
     * Ubicación para mostrar.
     *
     * <p>Primero {@code job_location} sin el "a través de LinkedIn" del final, porque en la v5
     * {@code job_city} a veces viene mal (un puesto en San José, Costa Rica, aparece con estado
     * California). La ciudad se descarta si el país es otro. Si no hay nada: "Costa Rica", o
     * "Todo el país" si es remota.
     */
    private static String location(JsonNode node) {
        String raw = text(node, "job_location");
        if (raw != null) {
            String cleaned = cleanLocation(raw);
            if (!cleaned.isEmpty()) {
                return cleaned;
            }
        }
        String city = text(node, "job_city");
        String country = text(node, "job_country");
        if (city != null && (country == null || "CR".equalsIgnoreCase(country))) {
            return city;
        }
        return node.path("job_is_remote").asBoolean(false) ? "Todo el país" : "Costa Rica";
    }

    /**
     * "Provincia de Alajuela, Alajuela  (y 1 ubicación más) · a través de LinkedIn" → "Alajuela".
     * Quita la fuente, el "(y N ubicaciones más)", el "Provincia de" y las partes repetidas.
     */
    static String cleanLocation(String raw) {
        String s = VIA.matcher(raw).replaceFirst("");
        s = MORE_PLACES.matcher(s).replaceAll("");
        s = PROVINCE.matcher(s).replaceAll("");
        java.util.LinkedHashSet<String> parts = new java.util.LinkedHashSet<>();
        for (String part : s.split(",")) {
            String p = EDGES.matcher(part).replaceAll("").trim().replaceAll("\\s+", " ");
            if (!p.isEmpty()) {
                parts.add(p);
            }
        }
        return String.join(", ", parts);
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

    /**
     * Tipo de jornada. Se prefiere {@code job_employment_types}, que trae códigos en inglés
     * ("FULLTIME"); {@code job_employment_type} viene en el idioma pedido ("Tiempo completo").
     */
    private static JobType jobType(JsonNode node) {
        JsonNode codes = node.path("job_employment_types");
        String type = codes.isArray() && !codes.isEmpty() && codes.get(0).isString()
                ? codes.get(0).asString()
                : text(node, "job_employment_type");
        if (type == null) {
            return JobType.FULL_TIME;
        }
        String upper = type.toUpperCase(Locale.ROOT).replace(" ", "").replace("-", "");
        if (upper.contains("INTERN") || upper.contains("PRÁCTICA") || upper.contains("PRACTICA")
                || upper.contains("PASANT")) {
            return JobType.INTERNSHIP;
        }
        if (upper.contains("PARTTIME") || upper.contains("MEDIOTIEMPO")) {
            return JobType.PART_TIME;
        }
        if (upper.contains("CONTRACTOR") || upper.contains("FREELANCE") || upper.contains("CONTRATISTA")) {
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

    /**
     * Identificador de la oferta para no importarla dos veces: MD5 de "título|empresa" en
     * minúscula.
     *
     * <p>No se usa {@code job_id} porque en la v5 de JSearch cambia en cada consulta para la misma
     * oferta, y cada importación la volvía a crear. V4__ofertas_sin_duplicados.sql calcula lo mismo
     * en SQL ({@code md5(lower(title) || '|' || lower(company))}) para las que ya estaban.
     */
    static String stableId(String title, String company) {
        String key = title.toLowerCase(Locale.ROOT) + "|" + company.toLowerCase(Locale.ROOT);
        try {
            byte[] hash = MessageDigest.getInstance("MD5").digest(key.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            // Toda JVM trae MD5; si no, es un problema de la instalación, no de los datos.
            throw new IllegalStateException(e);
        }
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
