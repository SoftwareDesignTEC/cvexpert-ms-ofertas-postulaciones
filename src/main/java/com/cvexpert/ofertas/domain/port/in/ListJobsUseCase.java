package com.cvexpert.ofertas.domain.port.in;

import com.cvexpert.ofertas.domain.model.Caller;
import com.cvexpert.ofertas.domain.model.JobFilter;
import com.cvexpert.ofertas.domain.model.PageQuery;
import com.cvexpert.ofertas.domain.model.PageResult;
import com.cvexpert.ofertas.domain.model.ScoredJobOffer;

/**
 * {@code GET /jobs}: ofertas vigentes, de la más nueva a la más vieja, con la compatibilidad de
 * cada una para quien consulta.
 */
public interface ListJobsUseCase {

    default PageResult<ScoredJobOffer> listJobs(Caller caller, PageQuery query) {
        return listJobs(caller, query, JobFilter.NONE);
    }

    /** Igual, pero solo las que pasan {@code filter}; {@code total} cuenta las filtradas. */
    PageResult<ScoredJobOffer> listJobs(Caller caller, PageQuery query, JobFilter filter);
}
