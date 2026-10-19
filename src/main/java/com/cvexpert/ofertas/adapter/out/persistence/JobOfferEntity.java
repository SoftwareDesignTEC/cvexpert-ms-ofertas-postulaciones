package com.cvexpert.ofertas.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.List;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Fila de {@code job_offers}. Es solo la forma de la tabla: el dominio nunca la ve, la recibe
 * convertida por {@link PersistenceMapper}. Los campos son de paquete porque solo el adaptador y el
 * mapper la tocan.
 *
 * <p>Los nombres de columna salen de los campos (Spring pasa {@code postedAt} a {@code posted_at}).
 */
@Entity
@Table(name = "job_offers")
class JobOfferEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    String title;
    String company;
    String companyLogoUrl;
    String location;
    String modality;
    String jobType;
    String sector;
    String description;

    // Listas de texto como arreglos de PostgreSQL (text[]).
    @JdbcTypeCode(SqlTypes.ARRAY)
    List<String> requirements;

    @JdbcTypeCode(SqlTypes.ARRAY)
    List<String> skills;

    Integer minExperienceYears;

    @Column(precision = 12, scale = 2)
    BigDecimal salaryMin;

    @Column(precision = 12, scale = 2)
    BigDecimal salaryMax;

    String salaryCurrency;

    long postedAt;

    /** Epoch milisegundos del borrado lógico; {@code null} si la oferta está vigente. */
    Long deletedAt;

    protected JobOfferEntity() {
        // Lo pide JPA.
    }
}
