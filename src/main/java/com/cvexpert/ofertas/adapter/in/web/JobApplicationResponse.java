package com.cvexpert.ofertas.adapter.in.web;

import com.cvexpert.ofertas.domain.model.JobApplication;

/**
 * {@code JobApplication} del contrato. Los ids van como texto y las fechas en epoch milisegundos
 * UTC, igual que espera la web y la app.
 */
public record JobApplicationResponse(
        String id,
        String userId,
        String jobOfferId,
        String jobTitle,
        String company,
        String companyLogoUrl,
        String location,
        String modality,
        String status,
        long appliedAt,
        long updatedAt,
        String notes) {

    static JobApplicationResponse from(JobApplication a) {
        return new JobApplicationResponse(String.valueOf(a.id()), a.userId(), String.valueOf(a.jobOfferId()),
                a.jobTitle(), a.company(), a.companyLogoUrl(), a.location(), a.modality().wireValue(),
                a.status().wireValue(), a.appliedAt(), a.updatedAt(), a.notes());
    }
}
