package com.cvexpert.ofertas.domain.model;

import com.cvexpert.ofertas.domain.exception.InvalidRequestException;
import java.util.List;

/**
 * Lo que se compara con cada oferta para calcular la compatibilidad. Sale del perfil del usuario
 * (servicio de Usuarios), así que lo manda el cliente y este servicio lo guarda.
 *
 * @param modalities modalidades que acepta; vacía significa que le da igual
 * @param jobTypes   tipos de jornada que acepta (su disponibilidad); vacía significa que le da igual
 */
public record MatchingProfile(
        List<String> skills,
        String location,
        List<Modality> modalities,
        List<JobType> jobTypes,
        List<String> sectors,
        int experienceYears) {

    public MatchingProfile {
        if (location == null || location.isBlank()) {
            throw new InvalidRequestException("Falta el campo profile.location");
        }
        if (experienceYears < 0) {
            throw new InvalidRequestException("profile.experienceYears no puede ser negativo");
        }
        skills = skills == null ? List.of() : List.copyOf(skills);
        modalities = modalities == null ? List.of() : List.copyOf(modalities);
        jobTypes = jobTypes == null ? List.of() : List.copyOf(jobTypes);
        sectors = sectors == null ? List.of() : List.copyOf(sectors);
    }
}
