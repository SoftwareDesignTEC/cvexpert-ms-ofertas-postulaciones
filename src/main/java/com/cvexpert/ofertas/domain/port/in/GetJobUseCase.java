package com.cvexpert.ofertas.domain.port.in;

import com.cvexpert.ofertas.domain.model.Caller;
import com.cvexpert.ofertas.domain.model.JobOffer;

/** {@code GET /jobs/{id}}. Una oferta borrada no existe para este caso de uso. */
public interface GetJobUseCase {

    JobOffer getJob(Caller caller, long id);
}
