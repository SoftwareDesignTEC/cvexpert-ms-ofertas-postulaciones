package com.cvexpert.ofertas.domain.port.out;

import com.cvexpert.ofertas.domain.exception.AlreadyAppliedException;
import com.cvexpert.ofertas.domain.model.JobApplication;
import com.cvexpert.ofertas.domain.model.PageQuery;
import com.cvexpert.ofertas.domain.model.PageResult;
import java.util.Optional;

/** Dónde viven las postulaciones. */
public interface JobApplicationRepository {

    /**
     * Guarda una postulación nueva (sin id) o los cambios de una que ya existe.
     *
     * @throws AlreadyAppliedException si ya hay otra postulación del mismo usuario a la misma
     *                                 oferta. Cubre el caso de dos solicitudes simultáneas que
     *                                 pasan la revisión previa del servicio.
     */
    JobApplication save(JobApplication application);

    Optional<JobApplication> findById(long id);

    boolean existsByUserAndJobOffer(String userId, long jobOfferId);

    /**
     * Postulaciones del usuario cuyo título, empresa o ubicación contienen el texto buscado, la
     * última movida primero.
     */
    PageResult<JobApplication> searchByUser(String userId, PageQuery query);

    void delete(long id);
}
