package com.cvexpert.ofertas.domain.model;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Calcula qué tan compatible es una oferta con un perfil, de 0 a 100. Cada criterio da una
 * fracción entre 0 y 1, se multiplica por su peso y el total se lleva a 100.
 *
 * <ul>
 *   <li><b>Habilidades:</b> parte de las habilidades que pide la oferta que la persona tiene, sin
 *       distinguir mayúsculas ni espacios. Si la oferta no pide ninguna, cuenta completo.</li>
 *   <li><b>Ubicación:</b> completo si la oferta es remota o si la ubicación coincide; si no, 0.</li>
 *   <li><b>Disponibilidad:</b> completo si el tipo de jornada de la oferta está entre los que
 *       acepta la persona, o si no indicó ninguno.</li>
 *   <li><b>Experiencia:</b> completo si cumple los años mínimos; si le faltan, proporcional (con 1
 *       de 2 años, la mitad).</li>
 * </ul>
 */
public class CompatibilityCalculator {

    private final MatchWeights weights;

    public CompatibilityCalculator(MatchWeights weights) {
        this.weights = weights;
    }

    public int score(JobOffer offer, MatchingProfile profile) {
        JobOfferDetails d = offer.details();
        double points = weights.skills() * skills(d, profile)
                + weights.location() * location(d, profile)
                + weights.availability() * availability(d, profile)
                + weights.experience() * experience(d, profile);
        return (int) Math.round(100 * points / weights.total());
    }

    static double skills(JobOfferDetails offer, MatchingProfile profile) {
        Set<String> wanted = normalized(offer.skills());
        if (wanted.isEmpty()) {
            return 1;
        }
        Set<String> has = normalized(profile.skills());
        long matched = wanted.stream().filter(has::contains).count();
        return (double) matched / wanted.size();
    }

    static double location(JobOfferDetails offer, MatchingProfile profile) {
        if (offer.modality() == Modality.REMOTE) {
            return 1;
        }
        return normalize(offer.location()).equals(normalize(profile.location())) ? 1 : 0;
    }

    static double availability(JobOfferDetails offer, MatchingProfile profile) {
        return profile.jobTypes().isEmpty() || profile.jobTypes().contains(offer.type()) ? 1 : 0;
    }

    static double experience(JobOfferDetails offer, MatchingProfile profile) {
        Integer required = offer.minExperienceYears();
        if (required == null || required == 0) {
            return 1;
        }
        return Math.min(1.0, (double) profile.experienceYears() / required);
    }

    private static Set<String> normalized(List<String> values) {
        return values.stream().map(CompatibilityCalculator::normalize).filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip().toLowerCase(Locale.ROOT);
    }
}
