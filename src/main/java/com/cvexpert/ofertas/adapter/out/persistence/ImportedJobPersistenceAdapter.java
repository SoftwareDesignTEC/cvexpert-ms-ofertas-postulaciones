package com.cvexpert.ofertas.adapter.out.persistence;

import com.cvexpert.ofertas.domain.model.ImportedJob;
import com.cvexpert.ofertas.domain.port.out.ImportedJobRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Implementa el puerto {@link ImportedJobRepository} sobre la misma tabla {@code job_offers}. */
@Component
class ImportedJobPersistenceAdapter implements ImportedJobRepository {

    private final JobOfferJpaRepository jpa;

    ImportedJobPersistenceAdapter(JobOfferJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    @Transactional
    public Upsert upsert(ImportedJob job) {
        var existing = jpa.findBySourceAndExternalId(job.source(), job.externalId());
        JobOfferEntity entity = existing.orElseGet(() -> {
            JobOfferEntity created = new JobOfferEntity();
            created.source = job.source();
            created.externalId = job.externalId();
            created.postedAt = job.postedAt();
            return created;
        });
        // Si ya estaba, se actualiza la descripción (la empresa pudo editar la oferta). Una oferta
        // que se había borrado se queda borrada: el importador no la revive.
        PersistenceMapper.copyDetails(job.details(), entity);
        JobOfferEntity saved = jpa.saveAndFlush(entity);
        return new Upsert(PersistenceMapper.toDomain(saved), existing.isEmpty());
    }

    @Override
    public int retireSource(String source, long deletedAt) {
        return jpa.markSourceDeleted(source, deletedAt);
    }
}
