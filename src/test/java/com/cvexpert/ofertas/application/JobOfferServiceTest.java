package com.cvexpert.ofertas.application;

import static com.cvexpert.ofertas.Fixtures.ADMIN;
import static com.cvexpert.ofertas.Fixtures.ANA;
import static com.cvexpert.ofertas.Fixtures.NOW;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.cvexpert.ofertas.Fixtures;
import com.cvexpert.ofertas.domain.exception.ForbiddenException;
import com.cvexpert.ofertas.domain.exception.NotFoundException;
import com.cvexpert.ofertas.domain.model.JobOffer;
import com.cvexpert.ofertas.domain.model.PageQuery;
import org.junit.jupiter.api.Test;

class JobOfferServiceTest {

    private final InMemoryRepositories.Offers offers = new InMemoryRepositories.Offers();
    private final JobOfferService service = new JobOfferService(offers, Fixtures.clockAt(NOW));

    @Test
    void adminCreatesAnOfferPostedNow() {
        JobOffer created = service.createJob(ADMIN, Fixtures.details("Desarrollador Android"));

        assertEquals(1L, created.id());
        assertEquals(NOW, created.postedAt());
        assertEquals(created, service.getJob(ANA, 1L));
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
        JobOfferService later = new JobOfferService(offers, Fixtures.clockAt(NOW + 60_000));

        JobOffer edited = later.updateJob(ADMIN, offer.id(), Fixtures.details("Dev Senior"));

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
}
