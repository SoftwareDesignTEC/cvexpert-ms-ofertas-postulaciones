package com.cvexpert.ofertas.adapter.out.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Consultas de Spring Data sobre {@code job_applications}. */
interface JobApplicationJpaRepository extends JpaRepository<JobApplicationEntity, Long> {

    boolean existsByUserIdAndJobOfferId(String userId, long jobOfferId);

    Page<JobApplicationEntity> findByUserId(String userId, Pageable pageable);

    /** {@code pattern} ya viene en minúscula, con comodines y con {@code !} como escape. */
    @Query("""
            select a from JobApplicationEntity a
            where a.userId = :userId
              and (lower(a.jobTitle) like :pattern escape '!'
                   or lower(a.company) like :pattern escape '!'
                   or lower(a.location) like :pattern escape '!')
            """)
    Page<JobApplicationEntity> searchByUser(@Param("userId") String userId, @Param("pattern") String pattern,
            Pageable pageable);
}
