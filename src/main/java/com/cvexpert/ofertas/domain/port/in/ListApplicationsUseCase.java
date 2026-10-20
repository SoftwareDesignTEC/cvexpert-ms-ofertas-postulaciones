package com.cvexpert.ofertas.domain.port.in;

import com.cvexpert.ofertas.domain.model.Caller;
import com.cvexpert.ofertas.domain.model.JobApplication;
import com.cvexpert.ofertas.domain.model.PageQuery;
import com.cvexpert.ofertas.domain.model.PageResult;

/** {@code GET /applications}: las postulaciones de quien llama, la última movida primero. */
public interface ListApplicationsUseCase {

    PageResult<JobApplication> listApplications(Caller caller, PageQuery query);
}
