package com.cvexpert.ofertas.domain.port.in;

import com.cvexpert.ofertas.domain.model.ApplicationStatus;
import com.cvexpert.ofertas.domain.model.Caller;
import com.cvexpert.ofertas.domain.model.JobApplication;

/**
 * {@code PUT /applications/{id}}: la persona mueve su propia tarjeta en el Kanban. No publica
 * evento.
 */
public interface MoveApplicationUseCase {

    JobApplication moveApplication(Caller caller, long id, ApplicationStatus status);
}
