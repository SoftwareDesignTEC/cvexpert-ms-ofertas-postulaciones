package com.cvexpert.ofertas.adapter.in.web;

import com.cvexpert.ofertas.domain.model.JobOffer;
import com.cvexpert.ofertas.domain.model.JobOfferDetails;
import com.cvexpert.ofertas.domain.model.Salary;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.util.List;

/** Cuerpos de las rutas de ofertas: {@code JobOfferInput} y {@code JobOffer} del contrato. */
final class JobOfferBodies {

    private JobOfferBodies() {
    }

    record SalaryBody(BigDecimal min, BigDecimal max, String currency) {

        static SalaryBody from(Salary s) {
            return s == null ? null : new SalaryBody(s.min(), s.max(), s.currency());
        }

        Salary toDomain() {
            return new Salary(min, max, currency);
        }
    }

    /** {@code JobOfferInput}: lo que manda un administrador al crear o editar una oferta. */
    record Input(
            String title,
            String company,
            String companyLogoUrl,
            String location,
            String modality,
            String type,
            String sector,
            String description,
            List<String> requirements,
            List<String> skills,
            Integer minExperienceYears,
            SalaryBody salary) {

        JobOfferDetails toDetails() {
            return new JobOfferDetails(title, company, companyLogoUrl, location,
                    WireValues.modality(modality, "modality"), WireValues.jobType(type, "type"), sector,
                    description, requirements, skills, minExperienceYears,
                    salary == null ? null : salary.toDomain());
        }
    }

    /**
     * {@code JobOffer}. Los campos opcionales vacíos no se mandan; en particular {@code matchScore}
     * falta cuando quien consulta no guardó su perfil, como dice el contrato.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    record Output(
            String id,
            String title,
            String company,
            String companyLogoUrl,
            String location,
            String modality,
            String type,
            String sector,
            String description,
            List<String> requirements,
            List<String> skills,
            Integer minExperienceYears,
            SalaryBody salary,
            Integer matchScore,
            long postedAt) {

        static Output from(JobOffer offer, Integer matchScore) {
            JobOfferDetails d = offer.details();
            return new Output(String.valueOf(offer.id()), d.title(), d.company(), d.companyLogoUrl(), d.location(),
                    d.modality().wireValue(), d.type().wireValue(), d.sector(), d.description(), d.requirements(),
                    d.skills(), d.minExperienceYears(), SalaryBody.from(d.salary()), matchScore, offer.postedAt());
        }
    }
}
