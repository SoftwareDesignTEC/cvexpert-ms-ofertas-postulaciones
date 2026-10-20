package com.cvexpert.ofertas.domain.port.in;

import com.cvexpert.ofertas.domain.model.ApplicationStatus;
import com.cvexpert.ofertas.domain.model.Caller;
import com.cvexpert.ofertas.domain.model.JobApplication;

/**
 * {@code PUT /admin/applications/{id}/status}: un administrador cambia el estado de la postulación
 * de otra persona. Si el estado cambia, publica {@code application.status_changed}.
 */
public interface AdminChangeStatusUseCase {

    JobApplication changeStatus(Caller caller, long id, ApplicationStatus status);
}
