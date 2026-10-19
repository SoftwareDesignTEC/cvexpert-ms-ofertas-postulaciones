package com.cvexpert.ofertas.adapter.out.persistence;

import com.cvexpert.ofertas.domain.exception.NotFoundException;
import com.cvexpert.ofertas.domain.model.JobOffer;
import com.cvexpert.ofertas.domain.model.PageQuery;
import com.cvexpert.ofertas.domain.model.PageResult;
import com.cvexpert.ofertas.domain.port.out.JobOfferRepository;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Implementa el puerto {@link JobOfferRepository} con PostgreSQL. */
@Component
class JobOfferPersistenceAdapter implements JobOfferRepository {

    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("postedAt"), Sort.Order.desc("id"));

    private final JobOfferJpaRepository jpa;

    JobOfferPersistenceAdapter(JobOfferJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    @Transactional
    public JobOffer save(JobOffer offer) {
        JobOfferEntity entity;
        if (offer.id() == null) {
            entity = new JobOfferEntity();
            entity.postedAt = offer.postedAt();
        } else {
            // Se carga la fila para conservar el borrado lógico y la fecha de publicación.
            entity = jpa.findByIdAndDeletedAtIsNull(offer.id())
                    .orElseThrow(() -> NotFoundException.jobOffer(offer.id()));
        }
        PersistenceMapper.copyDetails(offer.details(), entity);
        return PersistenceMapper.toDomain(jpa.saveAndFlush(entity));
    }

    @Override
    public Optional<JobOffer> findById(long id) {
        return jpa.findByIdAndDeletedAtIsNull(id).map(PersistenceMapper::toDomain);
    }

    @Override
    public PageResult<JobOffer> search(PageQuery query) {
        var pageable = QuerySupport.pageRequest(query, NEWEST_FIRST);
        Page<JobOfferEntity> page = query.hasSearch()
                ? jpa.searchActive(QuerySupport.contains(query.search()), pageable)
                : jpa.findByDeletedAtIsNull(pageable);
        return QuerySupport.toResult(page, query, PersistenceMapper::toDomain);
    }

    @Override
    public boolean markDeleted(long id, long deletedAt) {
        return jpa.markDeleted(id, deletedAt) == 1;
    }
}
