package com.cvexpert.ofertas.adapter.out.persistence;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

/** Consultas de Spring Data sobre {@code job_offers}. Todas ignoran las ofertas borradas. */
interface JobOfferJpaRepository extends JpaRepository<JobOfferEntity, Long> {

    Optional<JobOfferEntity> findByIdAndDeletedAtIsNull(long id);

    Page<JobOfferEntity> findByDeletedAtIsNull(Pageable pageable);

    /** {@code pattern} ya viene en minúscula, con comodines y con {@code !} como escape. */
    @Query("""
            select o from JobOfferEntity o
            where o.deletedAt is null
              and (lower(o.title) like :pattern escape '!'
                   or lower(o.company) like :pattern escape '!'
                   or lower(o.location) like :pattern escape '!')
            """)
    Page<JobOfferEntity> searchActive(@Param("pattern") String pattern, Pageable pageable);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("update JobOfferEntity o set o.deletedAt = :deletedAt where o.id = :id and o.deletedAt is null")
    int markDeleted(@Param("id") long id, @Param("deletedAt") long deletedAt);
}
