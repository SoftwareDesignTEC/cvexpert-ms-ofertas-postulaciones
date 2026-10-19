package com.cvexpert.ofertas.domain.model;

import com.cvexpert.ofertas.domain.exception.InvalidRequestException;
import java.util.List;

/**
 * Lo que describe una oferta: los campos de {@code JobOfferInput} del contrato. Es lo que manda un
 * administrador al crear o editar, y no incluye el id ni la fecha de publicación.
 *
 * <p>Las listas nunca son {@code null}: si no vienen quedan vacías.
 *
 * @param skills             habilidades que pide la oferta; se usan para la compatibilidad
 * @param minExperienceYears años de experiencia mínimos, o {@code null} si la oferta no pide
 * @param salary             {@code null} si la oferta no publica salario
 */
public record JobOfferDetails(
        String title,
        String company,
        String companyLogoUrl,
        String location,
        Modality modality,
        JobType type,
        String sector,
        String description,
        List<String> requirements,
        List<String> skills,
        Integer minExperienceYears,
        Salary salary) {

    public JobOfferDetails {
        requireText(title, "title");
        requireText(company, "company");
        requireText(location, "location");
        requireText(sector, "sector");
        requireText(description, "description");
        if (modality == null) {
            throw new InvalidRequestException("Falta el campo modality");
        }
        if (type == null) {
            throw new InvalidRequestException("Falta el campo type");
        }
        if (minExperienceYears != null && minExperienceYears < 0) {
            throw new InvalidRequestException("minExperienceYears no puede ser negativo");
        }
        requirements = requirements == null ? List.of() : List.copyOf(requirements);
        skills = skills == null ? List.of() : List.copyOf(skills);
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new InvalidRequestException("Falta el campo " + field);
        }
    }
}
