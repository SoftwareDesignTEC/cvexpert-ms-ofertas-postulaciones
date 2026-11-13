package com.cvexpert.ofertas.domain.port.in;

/**
 * {@code POST /jobs/refresh}: busca ofertas nuevas en la fuente externa en ese momento, sin
 * esperar a la corrida programada. Sirve para mostrar en vivo que las ofertas son reales.
 *
 * <p>Cada corrida gasta consultas del plan de JSearch, así que hay una espera mínima entre una y
 * otra.
 */
public interface RefreshJobsUseCase {

    /** Cuántas ofertas entraron: nuevas, actualizadas y búsquedas que fallaron. */
    record Result(int created, int updated, int failedQueries) {
    }

    Result refreshJobs();
}
