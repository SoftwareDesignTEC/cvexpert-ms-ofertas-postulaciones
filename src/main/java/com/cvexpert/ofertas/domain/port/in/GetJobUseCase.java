package com.cvexpert.ofertas.domain.port.in;

import com.cvexpert.ofertas.domain.model.Caller;
import com.cvexpert.ofertas.domain.model.ScoredJobOffer;

/** {@code GET /jobs/{id}}. Una oferta borrada no existe para este caso de uso. */
public interface GetJobUseCase {

    ScoredJobOffer getJob(Caller caller, long id);
}
