package com.cvexpert.ofertas.domain.port.out;

import com.cvexpert.ofertas.domain.model.JobOffer;
import com.cvexpert.ofertas.domain.model.Modality;
import com.cvexpert.ofertas.domain.model.PageQuery;
import com.cvexpert.ofertas.domain.model.PageResult;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Dónde viven las ofertas. Ningún método devuelve ofertas borradas. */
public interface JobOfferRepository {

    /** Guarda una oferta nueva (sin id) o los cambios de una que ya existe. Devuelve la guardada. */
    JobOffer save(JobOffer offer);

    Optional<JobOffer> findById(long id);

    /** Ofertas cuyo título, empresa o ubicación contienen el texto buscado, la más nueva primero. */
    default PageResult<JobOffer> search(PageQuery query) {
        return search(query, Set.of(), 0L);
    }

    /**
     * Igual, pero solo con esas modalidades (vacío = todas) y publicadas desde
     * {@code postedAfter} (epoch ms; 0 = sin límite).
     */
    PageResult<JobOffer> search(PageQuery query, Set<Modality> modalities, long postedAfter);

    /**
     * Todas las que pasan esos filtros, sin paginar, la más nueva primero. Para filtrar por
     * compatibilidad, que se calcula en Java y no en la base.
     */
    List<JobOffer> findAll(String search, Set<Modality> modalities, long postedAfter);

    /** Borra la oferta de forma lógica. Devuelve {@code false} si no existía o ya estaba borrada. */
    boolean markDeleted(long id, long deletedAt);
}
