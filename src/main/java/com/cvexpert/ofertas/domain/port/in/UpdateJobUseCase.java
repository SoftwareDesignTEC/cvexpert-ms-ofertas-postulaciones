package com.cvexpert.ofertas.domain.port.in;

import com.cvexpert.ofertas.domain.model.Caller;
import com.cvexpert.ofertas.domain.model.JobOffer;
import com.cvexpert.ofertas.domain.model.JobOfferDetails;

/**
 * {@code PUT /jobs/{id}}. Solo administradores. Las postulaciones que ya existen conservan su
 * copia del título y la empresa.
 */
public interface UpdateJobUseCase {

    JobOffer updateJob(Caller caller, long id, JobOfferDetails details);
}
