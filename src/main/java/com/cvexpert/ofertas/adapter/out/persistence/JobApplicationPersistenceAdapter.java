package com.cvexpert.ofertas.adapter.out.persistence;

import com.cvexpert.ofertas.domain.exception.AlreadyAppliedException;
import com.cvexpert.ofertas.domain.exception.NotFoundException;
import com.cvexpert.ofertas.domain.model.JobApplication;
import com.cvexpert.ofertas.domain.model.PageQuery;
import com.cvexpert.ofertas.domain.model.PageResult;
import com.cvexpert.ofertas.domain.port.out.JobApplicationRepository;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Implementa el puerto {@link JobApplicationRepository} con PostgreSQL. */
@Component
class JobApplicationPersistenceAdapter implements JobApplicationRepository {

    private static final Sort LAST_MOVED_FIRST = Sort.by(Sort.Order.desc("updatedAt"), Sort.Order.desc("id"));

    /** Nombre de la restricción UNIQUE de la migración V1. */
    private static final String ONE_PER_USER = "job_applications_one_per_user";

    private final JobApplicationJpaRepository jpa;

    JobApplicationPersistenceAdapter(JobApplicationJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    @Transactional
    public JobApplication save(JobApplication application) {
        JobApplicationEntity entity;
        if (application.id() == null) {
            entity = PersistenceMapper.toNewEntity(application);
        } else {
            entity = jpa.findById(application.id())
                    .orElseThrow(() -> NotFoundException.application(application.id()));
            PersistenceMapper.copyChanges(application, entity);
        }
        try {
            // saveAndFlush manda el INSERT ya, para que la violación del UNIQUE salga aquí.
            return PersistenceMapper.toDomain(jpa.saveAndFlush(entity));
        } catch (DataIntegrityViolationException e) {
            String cause = e.getMostSpecificCause().getMessage();
            if (cause != null && cause.contains(ONE_PER_USER)) {
                throw new AlreadyAppliedException();
            }
            throw e;
        }
    }

    @Override
    public Optional<JobApplication> findById(long id) {
        return jpa.findById(id).map(PersistenceMapper::toDomain);
    }

    @Override
    public boolean existsByUserAndJobOffer(String userId, long jobOfferId) {
        return jpa.existsByUserIdAndJobOfferId(userId, jobOfferId);
    }

    @Override
    public PageResult<JobApplication> searchByUser(String userId, PageQuery query) {
        var pageable = QuerySupport.pageRequest(query, LAST_MOVED_FIRST);
        Page<JobApplicationEntity> page = query.hasSearch()
                ? jpa.searchByUser(userId, QuerySupport.contains(query.search()), pageable)
                : jpa.findByUserId(userId, pageable);
        return QuerySupport.toResult(page, query, PersistenceMapper::toDomain);
    }

    @Override
    public void delete(long id) {
        jpa.deleteById(id);
    }
}
