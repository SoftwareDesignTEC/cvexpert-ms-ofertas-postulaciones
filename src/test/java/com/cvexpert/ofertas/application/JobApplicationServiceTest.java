package com.cvexpert.ofertas.application;

import static com.cvexpert.ofertas.Fixtures.ADMIN;
import static com.cvexpert.ofertas.Fixtures.ANA;
import static com.cvexpert.ofertas.Fixtures.LUIS;
import static com.cvexpert.ofertas.Fixtures.NOW;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cvexpert.ofertas.Fixtures;
import com.cvexpert.ofertas.domain.exception.AlreadyAppliedException;
import com.cvexpert.ofertas.domain.exception.ForbiddenException;
import com.cvexpert.ofertas.domain.exception.NotFoundException;
import com.cvexpert.ofertas.domain.model.ApplicationStatus;
import com.cvexpert.ofertas.domain.model.ApplicationStatusChanged;
import com.cvexpert.ofertas.domain.model.JobApplication;
import com.cvexpert.ofertas.domain.model.JobOffer;
import com.cvexpert.ofertas.domain.model.PageQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JobApplicationServiceTest {

    private final InMemoryRepositories.Offers offers = new InMemoryRepositories.Offers();
    private final InMemoryRepositories.Applications applications = new InMemoryRepositories.Applications();
    private final InMemoryRepositories.RecordingPublisher events = new InMemoryRepositories.RecordingPublisher();
    private final JobApplicationService service =
            new JobApplicationService(applications, offers, events, Fixtures.clockAt(NOW));

    private JobOffer offer;

    @BeforeEach
    void publishAnOffer() {
        offer = offers.save(JobOffer.publish(Fixtures.details("Desarrollador Android"), NOW - 1000));
    }

    @Test
    void applyingCreatesTheCardInApplied() {
        JobApplication a = service.apply(ANA, offer.id(), "Me interesa");

        assertEquals(ANA.userId(), a.userId());
        assertEquals(ApplicationStatus.APPLIED, a.status());
        assertEquals("Desarrollador Android", a.jobTitle());
        assertEquals(NOW, a.appliedAt());
        assertTrue(events.published.isEmpty(), "postularse no avisa a nadie");
    }

    @Test
    void oneApplicationPerUserAndOffer() {
        service.apply(ANA, offer.id(), null);

        assertThrows(AlreadyAppliedException.class, () -> service.apply(ANA, offer.id(), null));
        // Otra persona sí puede postularse a la misma oferta.
        service.apply(LUIS, offer.id(), null);
    }

    @Test
    void cannotApplyToAMissingOffer() {
        offers.markDeleted(offer.id(), NOW);
        assertThrows(NotFoundException.class, () -> service.apply(ANA, offer.id(), null));
    }

    @Test
    void movingYourOwnCardDoesNotPublish() {
        JobApplication a = service.apply(ANA, offer.id(), null);

        JobApplication moved = service.moveApplication(ANA, a.id(), ApplicationStatus.INTERVIEW);

        assertEquals(ApplicationStatus.INTERVIEW, moved.status());
        assertTrue(events.published.isEmpty());
    }

    @Test
    void someoneElsesApplicationLooksLikeItDoesNotExist() {
        JobApplication a = service.apply(ANA, offer.id(), null);

        assertThrows(NotFoundException.class, () -> service.getApplication(LUIS, a.id()));
        assertThrows(NotFoundException.class,
                () -> service.moveApplication(LUIS, a.id(), ApplicationStatus.REJECTED));
        assertThrows(NotFoundException.class, () -> service.withdraw(LUIS, a.id()));
        assertEquals(0, service.listApplications(LUIS, new PageQuery(1, 10, null)).total());
    }

    @Test
    void withdrawingRemovesTheCard() {
        JobApplication a = service.apply(ANA, offer.id(), null);

        service.withdraw(ANA, a.id());

        assertThrows(NotFoundException.class, () -> service.getApplication(ANA, a.id()));
    }

    @Test
    void adminChangePublishesOneEventForTheApplicant() {
        JobApplication a = service.apply(ANA, offer.id(), null);

        service.changeStatus(ADMIN, a.id(), ApplicationStatus.INTERVIEW);

        assertEquals(1, events.published.size());
        ApplicationStatusChanged event = events.published.get(0);
        assertEquals(ANA.userId(), event.userId());
        assertEquals(ApplicationStatus.APPLIED, event.previousStatus());
        assertEquals(ApplicationStatus.INTERVIEW, event.newStatus());
    }

    @Test
    void adminChangeToTheSameStatusDoesNothing() {
        JobApplication a = service.apply(ANA, offer.id(), null);
        int savesBefore = applications.saves;

        service.changeStatus(ADMIN, a.id(), ApplicationStatus.APPLIED);

        assertTrue(events.published.isEmpty());
        assertEquals(savesBefore, applications.saves, "no hay nada que guardar");
    }

    @Test
    void adminChangeNeedsTheRole() {
        JobApplication a = service.apply(ANA, offer.id(), null);
        assertThrows(ForbiddenException.class, () -> service.changeStatus(ANA, a.id(), ApplicationStatus.REJECTED));
        assertTrue(events.published.isEmpty());
    }
}
