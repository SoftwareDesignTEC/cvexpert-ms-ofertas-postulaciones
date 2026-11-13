package com.cvexpert.ofertas.domain.port.in;

import com.cvexpert.ofertas.domain.model.ApplicationStatus;
import com.cvexpert.ofertas.domain.model.Caller;
import com.cvexpert.ofertas.domain.model.JobApplication;

/**
 * {@code POST /applications}. Una sola postulación por usuario y oferta. No publica evento: el
 * cambio lo hizo la misma persona.
 */
public interface ApplyToJobUseCase {

    default JobApplication apply(Caller caller, long jobOfferId, String notes) {
        return apply(caller, jobOfferId, notes, ApplicationStatus.APPLIED);
    }

    /** Igual, pero la guarda directo en la columna {@code status}. */
    JobApplication apply(Caller caller, long jobOfferId, String notes, ApplicationStatus status);
}
