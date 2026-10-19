package com.cvexpert.ofertas.domain.port.out;

import com.cvexpert.ofertas.domain.model.JobOffer;
import com.cvexpert.ofertas.domain.model.PageQuery;
import com.cvexpert.ofertas.domain.model.PageResult;
import java.util.Optional;

/** Dónde viven las ofertas. Ningún método devuelve ofertas borradas. */
public interface JobOfferRepository {

    /** Guarda una oferta nueva (sin id) o los cambios de una que ya existe. Devuelve la guardada. */
    JobOffer save(JobOffer offer);

    Optional<JobOffer> findById(long id);

    /** Ofertas cuyo título, empresa o ubicación contienen el texto buscado, la más nueva primero. */
    PageResult<JobOffer> search(PageQuery query);

    /** Borra la oferta de forma lógica. Devuelve {@code false} si no existía o ya estaba borrada. */
    boolean markDeleted(long id, long deletedAt);
}
