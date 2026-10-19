package com.cvexpert.ofertas.adapter.out.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Fila de {@code job_applications}. La relación con la oferta es la llave foránea de la tabla; aquí
 * basta con el id, porque la postulación ya trae copia de lo que muestra de la oferta.
 */
@Entity
@Table(name = "job_applications")
class JobApplicationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    String userId;
    long jobOfferId;
    String jobTitle;
    String company;
    String companyLogoUrl;
    String location;
    String modality;
    String status;
    String notes;
    long appliedAt;
    long updatedAt;

    protected JobApplicationEntity() {
        // Lo pide JPA.
    }
}
