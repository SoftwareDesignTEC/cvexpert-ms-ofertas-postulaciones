package com.cvexpert.ofertas.adapter.out.persistence;

import com.cvexpert.ofertas.domain.model.ApplicationStatus;
import com.cvexpert.ofertas.domain.model.JobApplication;
import com.cvexpert.ofertas.domain.model.JobOffer;
import com.cvexpert.ofertas.domain.model.JobOfferDetails;
import com.cvexpert.ofertas.domain.model.JobType;
import com.cvexpert.ofertas.domain.model.Modality;
import com.cvexpert.ofertas.domain.model.Salary;
import java.util.ArrayList;

/** Convierte entre las filas de la base y los modelos del dominio. */
final class PersistenceMapper {

    private PersistenceMapper() {
    }

    static JobOffer toDomain(JobOfferEntity e) {
        Salary salary = e.salaryMin == null ? null : new Salary(e.salaryMin, e.salaryMax, e.salaryCurrency);
        JobOfferDetails details = new JobOfferDetails(e.title, e.company, e.companyLogoUrl, e.location,
                modality(e.modality), jobType(e.jobType), e.sector, e.description, e.requirements, e.skills,
                e.minExperienceYears, salary);
        return new JobOffer(e.id, details, e.postedAt);
    }

    /** Copia la descripción de la oferta en la fila. No toca el id ni el borrado lógico. */
    static void copyDetails(JobOfferDetails d, JobOfferEntity e) {
        e.title = d.title();
        e.company = d.company();
        e.companyLogoUrl = d.companyLogoUrl();
        e.location = d.location();
        e.modality = d.modality().wireValue();
        e.jobType = d.type().wireValue();
        e.sector = d.sector();
        e.description = d.description();
        e.requirements = new ArrayList<>(d.requirements());
        e.skills = new ArrayList<>(d.skills());
        e.minExperienceYears = d.minExperienceYears();
        Salary s = d.salary();
        e.salaryMin = s == null ? null : s.min();
        e.salaryMax = s == null ? null : s.max();
        e.salaryCurrency = s == null ? null : s.currency();
    }

    static JobApplication toDomain(JobApplicationEntity e) {
        return new JobApplication(e.id, e.userId, e.jobOfferId, e.jobTitle, e.company, e.companyLogoUrl,
                e.location, modality(e.modality), status(e.status), e.appliedAt, e.updatedAt, e.notes);
    }

    static JobApplicationEntity toNewEntity(JobApplication a) {
        JobApplicationEntity e = new JobApplicationEntity();
        e.userId = a.userId();
        e.jobOfferId = a.jobOfferId();
        e.jobTitle = a.jobTitle();
        e.company = a.company();
        e.companyLogoUrl = a.companyLogoUrl();
        e.location = a.location();
        e.modality = a.modality().wireValue();
        e.appliedAt = a.appliedAt();
        copyChanges(a, e);
        return e;
    }

    /** Lo único que cambia de una postulación después de creada: estado, notas y fecha de cambio. */
    static void copyChanges(JobApplication a, JobApplicationEntity e) {
        e.status = a.status().wireValue();
        e.notes = a.notes();
        e.updatedAt = a.updatedAt();
    }

    // Los CHECK de la tabla garantizan valores válidos; si aparece otro, la base se tocó a mano.
    private static Modality modality(String value) {
        return Modality.fromWireValue(value)
                .orElseThrow(() -> new IllegalStateException("Modalidad desconocida en la base: " + value));
    }

    private static JobType jobType(String value) {
        return JobType.fromWireValue(value)
                .orElseThrow(() -> new IllegalStateException("Tipo de oferta desconocido en la base: " + value));
    }

    private static ApplicationStatus status(String value) {
        return ApplicationStatus.fromWireValue(value)
                .orElseThrow(() -> new IllegalStateException("Estado desconocido en la base: " + value));
    }
}
