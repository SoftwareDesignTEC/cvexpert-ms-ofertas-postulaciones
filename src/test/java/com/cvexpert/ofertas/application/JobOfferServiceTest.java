package com.cvexpert.ofertas.application;

import static com.cvexpert.ofertas.Fixtures.ADMIN;
import static com.cvexpert.ofertas.Fixtures.ANA;
import static com.cvexpert.ofertas.Fixtures.LUIS;
import static com.cvexpert.ofertas.Fixtures.NOW;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cvexpert.ofertas.Fixtures;
import com.cvexpert.ofertas.domain.exception.ForbiddenException;
import com.cvexpert.ofertas.domain.exception.NotFoundException;
import com.cvexpert.ofertas.domain.model.AlertSettings;
import com.cvexpert.ofertas.domain.model.CompatibilityCalculator;
import com.cvexpert.ofertas.domain.model.JobOffer;
import com.cvexpert.ofertas.domain.model.JobType;
import com.cvexpert.ofertas.domain.model.MatchWeights;
import com.cvexpert.ofertas.domain.model.MatchingProfile;
import com.cvexpert.ofertas.domain.model.NewJobMatched;
import com.cvexpert.ofertas.domain.model.PageQuery;
import com.cvexpert.ofertas.domain.model.ScoredJobOffer;
import java.time.Clock;
import java.util.List;
import org.junit.jupiter.api.Test;

class JobOfferServiceTest {

    /** Cumple todo lo que pide la oferta de {@link Fixtures#details}: compatibilidad 100. */
    static final MatchingProfile PERFECT =
            new MatchingProfile(List.of("kotlin", " GIT "), "san josé", List.of(), List.of(JobType.FULL_TIME), List.of(), 2);

    /** Tiene la mitad de las habilidades y nada más: 0.5 × 40 = 20. */
    static final MatchingProfile WEAK =
            new MatchingProfile(List.of("Kotlin"), "Cartago", List.of(), List.of(JobType.PART_TIME), List.of(), 0);

    private final InMemoryRepositories.Offers offers = new InMemoryRepositories.Offers();
    private final InMemoryRepositories.Alerts alerts = new InMemoryRepositories.Alerts();
    private final InMemoryRepositories.RecordingPublisher events = new InMemoryRepositories.RecordingPublisher();
    private final JobOfferService service = serviceAt(Fixtures.clockAt(NOW));

    private JobOfferService serviceAt(Clock clock) {
        return new JobOfferService(offers, alerts, new CompatibilityCalculator(MatchWeights.DEFAULT), events, clock);
    }

    private void saveAlerts(String userId, boolean enabled, int minMatch, MatchingProfile profile) {
        alerts.save(new AlertSettings(userId, enabled, minMatch, profile, NOW));
    }

    @Test
    void adminCreatesAnOfferPostedNow() {
        JobOffer created = service.createJob(ADMIN, Fixtures.details("Desarrollador Android"));

        assertEquals(1L, created.id());
        assertEquals(NOW, created.postedAt());
        assertEquals(created, service.getJob(ANA, 1L).offer());
    }

    @Test
    void onlyAdminsCreateEditOrDelete() {
        JobOffer offer = service.createJob(ADMIN, Fixtures.details("Dev"));

        assertThrows(ForbiddenException.class, () -> service.createJob(ANA, Fixtures.details("Dev")));
        assertThrows(ForbiddenException.class, () -> service.updateJob(ANA, offer.id(), Fixtures.details("X")));
        assertThrows(ForbiddenException.class, () -> service.deleteJob(ANA, offer.id()));
    }

    @Test
    void editingKeepsIdAndPostedAt() {
        JobOffer offer = service.createJob(ADMIN, Fixtures.details("Dev"));

        JobOffer edited = serviceAt(Fixtures.clockAt(NOW + 60_000)).updateJob(ADMIN, offer.id(), Fixtures.details("Dev Senior"));

        assertEquals(offer.id(), edited.id());
        assertEquals(NOW, edited.postedAt());
        assertEquals("Dev Senior", edited.details().title());
    }

    @Test
    void aDeletedOfferNoLongerExists() {
        JobOffer offer = service.createJob(ADMIN, Fixtures.details("Dev"));

        service.deleteJob(ADMIN, offer.id());

        assertThrows(NotFoundException.class, () -> service.getJob(ANA, offer.id()));
        assertThrows(NotFoundException.class, () -> service.deleteJob(ADMIN, offer.id()));
        assertEquals(0, service.listJobs(ANA, new PageQuery(1, 10, null)).total());
    }

    @Test
    void unknownOfferIsNotFound() {
        NotFoundException e = assertThrows(NotFoundException.class, () -> service.getJob(ANA, 99L));
        assertEquals("JOB_NOT_FOUND", e.code());
        assertThrows(NotFoundException.class, () -> service.updateJob(ADMIN, 99L, Fixtures.details("X")));
    }

    @Test
    void matchScoreDependsOnWhoAsksAndIsMissingWithoutAProfile() {
        JobOffer offer = service.createJob(ADMIN, Fixtures.details("Dev"));
        saveAlerts(ANA.userId(), false, 0, PERFECT);
        saveAlerts(LUIS.userId(), false, 0, WEAK);

        assertEquals(100, service.getJob(ANA, offer.id()).matchScore());
        assertEquals(20, service.getJob(LUIS, offer.id()).matchScore());
        assertNull(service.getJob(ADMIN, offer.id()).matchScore(), "sin perfil guardado no hay matchScore");

        ScoredJobOffer listed = service.listJobs(ANA, new PageQuery(1, 10, null)).items().get(0);
        assertEquals(100, listed.matchScore());
    }

    @Test
    void creatingAnOfferNotifiesOnlyUsersAboveTheirMinimum() {
        saveAlerts(ANA.userId(), true, 70, PERFECT);       // 100 ≥ 70: recibe
        saveAlerts(LUIS.userId(), true, 70, WEAK);         // 20 < 70: no recibe
        saveAlerts("sin-alertas", false, 0, PERFECT);      // alertas apagadas: no recibe
        saveAlerts("justo", true, 20, WEAK);               // 20 ≥ 20: el mínimo cuenta como alcanzado

        JobOffer offer = service.createJob(ADMIN, Fixtures.details("Desarrollador Android"));

        assertEquals(List.of(ANA.userId(), "justo"), events.newJobs.stream().map(NewJobMatched::userId).toList());
        NewJobMatched toAna = events.newJobs.get(0);
        assertEquals(offer.id(), toAna.jobOfferId());
        assertEquals("Desarrollador Android", toAna.title());
        assertEquals(100, toAna.matchScore());
        assertEquals(NOW, toAna.occurredAt());
    }

    @Test
    void editingAnOfferDoesNotNotifyAgain() {
        saveAlerts(ANA.userId(), true, 0, PERFECT);
        JobOffer offer = service.createJob(ADMIN, Fixtures.details("Dev"));
        events.newJobs.clear();

        service.updateJob(ADMIN, offer.id(), Fixtures.details("Dev Senior"));

        assertTrue(events.newJobs.isEmpty());
    }
}
