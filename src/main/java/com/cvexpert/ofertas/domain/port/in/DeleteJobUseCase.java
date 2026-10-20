package com.cvexpert.ofertas.domain.port.in;

import com.cvexpert.ofertas.domain.model.Caller;

/**
 * {@code DELETE /jobs/{id}}. Solo administradores. El borrado es lógico: la oferta deja de
 * aparecer, pero las postulaciones a ella siguen apuntando a un id válido.
 */
public interface DeleteJobUseCase {

    void deleteJob(Caller caller, long id);
}
