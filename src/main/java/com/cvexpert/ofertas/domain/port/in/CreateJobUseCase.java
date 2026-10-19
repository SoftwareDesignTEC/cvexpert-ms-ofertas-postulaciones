package com.cvexpert.ofertas.domain.port.in;

import com.cvexpert.ofertas.domain.model.Caller;
import com.cvexpert.ofertas.domain.model.JobOffer;
import com.cvexpert.ofertas.domain.model.JobOfferDetails;

/** {@code POST /jobs}. Solo administradores. */
public interface CreateJobUseCase {

    JobOffer createJob(Caller caller, JobOfferDetails details);
}
