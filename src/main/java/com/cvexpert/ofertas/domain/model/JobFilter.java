package com.cvexpert.ofertas.domain.model;

import com.cvexpert.ofertas.domain.exception.InvalidRequestException;
import java.util.Set;

/**
 * Filtros opcionales de {@code GET /jobs}, además de la búsqueda por texto de {@link PageQuery}.
 *
 * @param modalities      modalidades aceptadas; vacío = todas.
 * @param postedSinceDays solo las publicadas en los últimos N días (1 a 365), o {@code null}.
 * @param minMatch        compatibilidad mínima con el perfil de quien consulta (0 a 100), o
 *                        {@code null}. Una oferta sin compatibilidad (el usuario no tiene perfil)
 *                        no la alcanza, salvo con 0.
 */
public record JobFilter(Set<Modality> modalities, Integer postedSinceDays, Integer minMatch) {

    public static final int MAX_DAYS = 365;
    public static final JobFilter NONE = new JobFilter(Set.of(), null, null);

    public JobFilter {
        modalities = modalities == null ? Set.of() : Set.copyOf(modalities);
        if (postedSinceDays != null && (postedSinceDays < 1 || postedSinceDays > MAX_DAYS)) {
            throw new InvalidRequestException("postedSinceDays tiene que estar entre 1 y " + MAX_DAYS);
        }
        if (minMatch != null && (minMatch < 0 || minMatch > 100)) {
            throw new InvalidRequestException("minMatch tiene que estar entre 0 y 100");
        }
    }

    /** Desde qué momento (epoch ms) cuenta una oferta, o 0 si no se filtra por fecha. */
    public long postedAfter(long nowMillis) {
        return postedSinceDays == null ? 0L : nowMillis - postedSinceDays * DAY_MS;
    }

    /** Si la compatibilidad {@code score} pasa el mínimo pedido. */
    public boolean accepts(Integer score) {
        if (minMatch == null || minMatch == 0) {
            return true;
        }
        return score != null && score >= minMatch;
    }

    public boolean hasMinMatch() {
        return minMatch != null && minMatch > 0;
    }

    private static final long DAY_MS = 24L * 60 * 60 * 1000;
}
