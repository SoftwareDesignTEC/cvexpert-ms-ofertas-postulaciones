package com.cvexpert.ofertas.domain.port.in;

import com.cvexpert.ofertas.domain.model.Caller;
import com.cvexpert.ofertas.domain.model.JobOffer;
import com.cvexpert.ofertas.domain.model.PageQuery;
import com.cvexpert.ofertas.domain.model.PageResult;

/** {@code GET /jobs}: ofertas vigentes, de la más nueva a la más vieja. */
public interface ListJobsUseCase {

    PageResult<JobOffer> listJobs(Caller caller, PageQuery query);
}
