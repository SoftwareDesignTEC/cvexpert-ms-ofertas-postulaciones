package com.cvexpert.ofertas.domain.port.out;

import com.cvexpert.ofertas.domain.model.ImportedJob;

/** Dónde se guardan las ofertas reales que trae el importador. */
public interface ImportedJobRepository {

    /**
     * Guarda la oferta o, si ya estaba (misma fuente e id externo), actualiza su descripción. La
     * fecha de publicación y el id no cambian, así las postulaciones siguen apuntando a la misma.
     *
     * @return {@code true} si la oferta es nueva
     */
    boolean upsert(ImportedJob job);

    /**
     * Borra de forma lógica todas las ofertas vigentes de una fuente (se usa para retirar las de
     * demostración cuando ya hay ofertas reales).
     *
     * @return cuántas se borraron
     */
    int retireSource(String source, long deletedAt);
}
