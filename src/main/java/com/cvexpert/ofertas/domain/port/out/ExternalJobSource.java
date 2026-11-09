package com.cvexpert.ofertas.domain.port.out;

import com.cvexpert.ofertas.domain.model.ImportedJob;
import com.cvexpert.ofertas.domain.model.JobSearchQuery;
import java.util.List;

/**
 * Una fuente de ofertas reales (hoy JSearch). El dominio no sabe cómo se consulta: solo pide una
 * búsqueda y recibe las ofertas ya convertidas a su modelo.
 */
public interface ExternalJobSource {

    /** Nombre con que se guardan las ofertas de esta fuente (columna {@code source}). */
    String name();

    /**
     * Ofertas que encuentra la búsqueda. Las que vienen incompletas (sin título o sin empresa) ya
     * se descartaron.
     *
     * @throws RuntimeException si la fuente no responde o responde con error; el importador lo
     *                          registra y sigue con la siguiente búsqueda
     */
    List<ImportedJob> search(JobSearchQuery query);
}
