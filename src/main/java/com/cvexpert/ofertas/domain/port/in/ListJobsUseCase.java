package com.cvexpert.ofertas.domain.port.in;

import com.cvexpert.ofertas.domain.model.Caller;
import com.cvexpert.ofertas.domain.model.PageQuery;
import com.cvexpert.ofertas.domain.model.PageResult;
import com.cvexpert.ofertas.domain.model.ScoredJobOffer;

/**
 * {@code GET /jobs}: ofertas vigentes, de la más nueva a la más vieja, con la compatibilidad de
 * cada una para quien consulta.
 */
public interface ListJobsUseCase {

    PageResult<ScoredJobOffer> listJobs(Caller caller, PageQuery query);
}
