package com.cvexpert.ofertas.domain.port.in;

import com.cvexpert.ofertas.domain.model.Caller;
import com.cvexpert.ofertas.domain.model.JobApplication;

/** {@code GET /applications/{id}}. La postulación de otro usuario no existe para quien llama. */
public interface GetApplicationUseCase {

    JobApplication getApplication(Caller caller, long id);
}
