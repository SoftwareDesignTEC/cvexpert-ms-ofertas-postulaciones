package com.cvexpert.ofertas.adapter.in.web;

import static com.cvexpert.ofertas.adapter.in.web.WireValues.required;

import com.cvexpert.ofertas.domain.model.AlertSettings;
import com.cvexpert.ofertas.domain.model.JobType;
import com.cvexpert.ofertas.domain.model.MatchingProfile;
import com.cvexpert.ofertas.domain.model.Modality;
import java.util.List;

/** Cuerpos de {@code /alerts}: {@code AlertSettingsInput}, {@code AlertSettings} y {@code MatchingProfile}. */
final class AlertBodies {

    private AlertBodies() {
    }

    /** {@code MatchingProfile}. Los nombres son los del perfil de la Fase I, iguales en la web y la app. */
    record Profile(
            List<String> skills,
            String location,
            List<String> modalities,
            List<String> jobTypes,
            List<String> sectors,
            Integer experienceYears) {

        MatchingProfile toDomain() {
            required(skills, "profile.skills");
            required(modalities, "profile.modalities");
            required(jobTypes, "profile.jobTypes");
            return new MatchingProfile(skills, location,
                    WireValues.modalities(modalities, "profile.modalities"),
                    WireValues.jobTypes(jobTypes, "profile.jobTypes"),
                    sectors, required(experienceYears, "profile.experienceYears"));
        }

        static Profile from(MatchingProfile p) {
            return new Profile(p.skills(), p.location(), p.modalities().stream().map(Modality::wireValue).toList(),
                    p.jobTypes().stream().map(JobType::wireValue).toList(), p.sectors(), p.experienceYears());
        }
    }

    /** {@code AlertSettingsInput}: los tres campos son obligatorios. */
    record Input(Boolean enabled, Integer minMatch, Profile profile) {
    }

    /** {@code AlertSettings}: lo guardado más la fecha del último cambio. */
    record Output(boolean enabled, int minMatch, Profile profile, long updatedAt) {

        static Output from(AlertSettings s) {
            return new Output(s.enabled(), s.minMatch(), Profile.from(s.profile()), s.updatedAt());
        }
    }
}
