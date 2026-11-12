package com.cvexpert.ofertas.adapter.out.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

/** Consultas de Spring Data sobre {@code job_offers}. Todas ignoran las ofertas borradas. */
interface JobOfferJpaRepository extends JpaRepository<JobOfferEntity, Long> {

    Optional<JobOfferEntity> findByIdAndDeletedAtIsNull(long id);

    /** La oferta importada con ese id externo, borrada o no (para no volver a crearla). */
    Optional<JobOfferEntity> findBySourceAndExternalId(String source, String externalId);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("update JobOfferEntity o set o.deletedAt = :deletedAt where o.source = :source and o.deletedAt is null")
    int markSourceDeleted(@Param("source") String source, @Param("deletedAt") long deletedAt);

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

    /**
     * Búsqueda con filtros. Sin parámetros opcionales a propósito (Postgres no sabe el tipo de un
     * null): sin texto llega "%", sin modalidad llegan las tres y sin fecha llega 0.
     */
    @Query(FILTERED)
    Page<JobOfferEntity> searchFiltered(@Param("pattern") String pattern,
            @Param("modalities") Collection<String> modalities, @Param("postedAfter") long postedAfter,
            Pageable pageable);

    @Query(FILTERED)
    List<JobOfferEntity> findFiltered(@Param("pattern") String pattern,
            @Param("modalities") Collection<String> modalities, @Param("postedAfter") long postedAfter, Sort sort);

    String FILTERED = """
            select o from JobOfferEntity o
            where o.deletedAt is null
              and (lower(o.title) like :pattern escape '!'
                   or lower(o.company) like :pattern escape '!'
                   or lower(o.location) like :pattern escape '!')
              and o.modality in :modalities
              and o.postedAt >= :postedAfter
            """;

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("update JobOfferEntity o set o.deletedAt = :deletedAt where o.id = :id and o.deletedAt is null")
    int markDeleted(@Param("id") long id, @Param("deletedAt") long deletedAt);
}
