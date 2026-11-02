package com.cvexpert.ofertas.domain.model;

import static com.cvexpert.ofertas.Fixtures.NOW;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Pruebas del cálculo de compatibilidad con los pesos por defecto: habilidades 40, ubicación 20,
 * disponibilidad 20 y experiencia 20.
 */
class CompatibilityCalculatorTest {

    private final CompatibilityCalculator calculator = new CompatibilityCalculator(MatchWeights.DEFAULT);

    private static JobOffer offer(Modality modality, String location, JobType type, List<String> skills,
            Integer minExperience) {
        return new JobOffer(1L, new JobOfferDetails("Dev", "Acme", null, location, modality, type, "Tecnología",
                "Desc", null, skills, minExperience, null), NOW);
    }

    private static MatchingProfile profile(List<String> skills, String location, List<JobType> jobTypes,
            int experience) {
        return new MatchingProfile(skills, location, null, jobTypes, null, experience);
    }

    @Test
    void meetingEverythingIs100() {
        JobOffer o = offer(Modality.ON_SITE, "Cartago", JobType.FULL_TIME, List.of("Java", "SQL"), 2);
        MatchingProfile p = profile(List.of("java", "sql", "docker"), " cartago ", List.of(JobType.FULL_TIME), 3);

        assertEquals(100, calculator.score(o, p));
    }

    @Test
    void meetingNothingIs0() {
        JobOffer o = offer(Modality.ON_SITE, "Cartago", JobType.FULL_TIME, List.of("Java"), 2);
        MatchingProfile p = profile(List.of("Python"), "Limón", List.of(JobType.PART_TIME), 0);

        assertEquals(0, calculator.score(o, p));
    }

    @Test
    void skillsCountByTheShareTheOfferAsksFor() {
        JobOffer o = offer(Modality.ON_SITE, "Cartago", JobType.FULL_TIME, List.of("Java", "SQL", "Git", "Docker"), null);
        MatchingProfile p = profile(List.of("Java"), "Limón", List.of(JobType.PART_TIME), 0);

        // 1 de 4 habilidades (0.25 × 40 = 10) más experiencia completa porque no pide (20).
        assertEquals(30, calculator.score(o, p));
    }

    @Test
    void aRemoteOfferMatchesAnyLocation() {
        JobOffer o = offer(Modality.REMOTE, "San José", JobType.FULL_TIME, List.of(), null);
        MatchingProfile p = profile(List.of(), "Guanacaste", List.of(JobType.FULL_TIME), 0);

        assertEquals(100, calculator.score(o, p));
    }

    @Test
    void noPreferredJobTypesMeansAnyIsFine() {
        JobOffer o = offer(Modality.ON_SITE, "Cartago", JobType.INTERNSHIP, List.of(), null);
        MatchingProfile p = profile(List.of(), "Limón", List.of(), 0);

        // Habilidades (no pide), disponibilidad (le da igual) y experiencia (no pide): 80.
        assertEquals(80, calculator.score(o, p));
    }

    @Test
    void missingExperienceIsProportional() {
        JobOffer o = offer(Modality.REMOTE, "Cartago", JobType.FULL_TIME, List.of(), 4);
        MatchingProfile p = profile(List.of(), "Cartago", List.of(JobType.FULL_TIME), 1);

        // Todo completo menos experiencia: 1 de 4 años da 0.25 × 20 = 5.
        assertEquals(85, calculator.score(o, p));
    }

    @Test
    void weightsAreNormalized() {
        JobOffer o = offer(Modality.ON_SITE, "Cartago", JobType.FULL_TIME, List.of("Java", "SQL"), null);
        MatchingProfile p = profile(List.of("Java"), "Limón", List.of(JobType.PART_TIME), 0);
        CompatibilityCalculator onlySkills = new CompatibilityCalculator(new MatchWeights(1, 0, 0, 0));

        assertEquals(50, onlySkills.score(o, p));
    }

    @Test
    void weightsCannotBeNegativeOrAllZero() {
        assertThrows(IllegalArgumentException.class, () -> new MatchWeights(-1, 20, 20, 20));
        assertThrows(IllegalArgumentException.class, () -> new MatchWeights(0, 0, 0, 0));
    }
}
