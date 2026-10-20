package com.cvexpert.ofertas.adapter.out.persistence;

import static com.cvexpert.ofertas.Fixtures.ANA;
import static com.cvexpert.ofertas.Fixtures.LUIS;
import static com.cvexpert.ofertas.Fixtures.NOW;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cvexpert.ofertas.Fixtures;
import com.cvexpert.ofertas.TestcontainersConfiguration;
import com.cvexpert.ofertas.domain.exception.AlreadyAppliedException;
import com.cvexpert.ofertas.domain.model.ApplicationStatus;
import com.cvexpert.ofertas.domain.model.JobApplication;
import com.cvexpert.ofertas.domain.model.JobOffer;
import com.cvexpert.ofertas.domain.model.JobOfferDetails;
import com.cvexpert.ofertas.domain.model.JobType;
import com.cvexpert.ofertas.domain.model.Modality;
import com.cvexpert.ofertas.domain.model.PageQuery;
import com.cvexpert.ofertas.domain.model.PageResult;
import com.cvexpert.ofertas.domain.port.out.JobApplicationRepository;
import com.cvexpert.ofertas.domain.port.out.JobOfferRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

/**
 * Prueba los adaptadores contra un PostgreSQL 18 real (Testcontainers), con el esquema que crea
 * Flyway. Así se comprueba que la migración, el mapeo JPA y las consultas calzan entre sí.
 *
 * <p>Cada prueba corre en una transacción que se deshace al final, así no se ensucian entre sí.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class PersistenceAdaptersTest {

    @Autowired
    JobOfferRepository offers;

    @Autowired
    JobApplicationRepository applications;

    private JobOffer publish(String title, long postedAt) {
        return offers.save(JobOffer.publish(Fixtures.details(title), postedAt));
    }

    @Test
    void anOfferComesBackExactlyAsSaved() {
        JobOffer saved = publish("Desarrollador Android", NOW);

        JobOffer read = offers.findById(saved.id()).orElseThrow();

        assertEquals(saved, read);
        assertEquals(List.of("Kotlin", "Git"), read.details().skills());
        assertEquals(0, read.details().salary().min().compareTo(new java.math.BigDecimal("600000")));
    }

    @Test
    void anOfferWithoutOptionalFieldsIsValid() {
        JobOfferDetails minimal = new JobOfferDetails("Pasantía", "Acme", null, "Heredia", Modality.REMOTE,
                JobType.INTERNSHIP, "Tecnología", "Desc", null, null, null, null);

        JobOffer read = offers.findById(offers.save(JobOffer.publish(minimal, NOW)).id()).orElseThrow();

        assertEquals(minimal, read.details());
    }

    @Test
    void editingKeepsPostedAt() {
        JobOffer saved = publish("Dev", NOW);

        offers.save(new JobOffer(saved.id(), Fixtures.details("Dev Senior"), NOW + 999));

        JobOffer read = offers.findById(saved.id()).orElseThrow();
        assertEquals("Dev Senior", read.details().title());
        assertEquals(NOW, read.postedAt());
    }

    @Test
    void searchIgnoresCaseOrdersNewestFirstAndPaginates() {
        publish("Desarrollador Android", NOW);
        publish("Diseñador UX", NOW + 1);
        publish("Desarrollador Web", NOW + 2);

        PageResult<JobOffer> page = offers.search(new PageQuery(1, 1, "DESARROLLADOR"));

        assertEquals(2, page.total());
        assertEquals(1, page.items().size());
        assertEquals("Desarrollador Web", page.items().get(0).details().title());
        assertEquals("Desarrollador Android",
                offers.search(new PageQuery(2, 1, "desarrollador")).items().get(0).details().title());
    }

    @Test
    void searchTreatsPercentAsText() {
        publish("Remoto 100%", NOW);
        publish("Presencial", NOW);

        assertEquals(1, offers.search(new PageQuery(1, 10, "100%")).total());
        assertEquals(0, offers.search(new PageQuery(1, 10, "%x")).total());
    }

    @Test
    void aDeletedOfferDisappearsButItsApplicationsStay() {
        JobOffer offer = publish("Dev", NOW);
        JobApplication a = applications.save(JobApplication.apply(ANA.userId(), offer, null, NOW));

        assertTrue(offers.markDeleted(offer.id(), NOW + 1));
        assertFalse(offers.markDeleted(offer.id(), NOW + 2), "ya estaba borrada");

        assertTrue(offers.findById(offer.id()).isEmpty());
        assertEquals(0, offers.search(new PageQuery(1, 10, null)).total());
        assertEquals(offer.id(), applications.findById(a.id()).orElseThrow().jobOfferId());
    }

    @Test
    void anApplicationComesBackExactlyAsSaved() {
        JobOffer offer = publish("Dev", NOW);
        JobApplication saved = applications.save(JobApplication.apply(ANA.userId(), offer, "Nota", NOW));

        assertEquals(saved, applications.findById(saved.id()).orElseThrow());
        assertTrue(applications.existsByUserAndJobOffer(ANA.userId(), offer.id()));
        assertFalse(applications.existsByUserAndJobOffer(LUIS.userId(), offer.id()));
    }

    @Test
    void theUniqueConstraintBecomesAlreadyApplied() {
        JobOffer offer = publish("Dev", NOW);
        applications.save(JobApplication.apply(ANA.userId(), offer, null, NOW));

        assertThrows(AlreadyAppliedException.class,
                () -> applications.save(JobApplication.apply(ANA.userId(), offer, null, NOW)));
    }

    @Test
    void theForeignKeyRejectsAMissingOffer() {
        JobOffer ghost = new JobOffer(999_999L, Fixtures.details("No existe"), NOW);

        assertThrows(DataIntegrityViolationException.class,
                () -> applications.save(JobApplication.apply(ANA.userId(), ghost, null, NOW)));
    }

    @Test
    void movingSavesStatusAndUpdatedAt() {
        JobOffer offer = publish("Dev", NOW);
        JobApplication a = applications.save(JobApplication.apply(ANA.userId(), offer, null, NOW));

        applications.save(a.moveTo(ApplicationStatus.IN_PROGRESS, NOW + 10));

        JobApplication read = applications.findById(a.id()).orElseThrow();
        assertEquals(ApplicationStatus.IN_PROGRESS, read.status());
        assertEquals(NOW + 10, read.updatedAt());
        assertEquals(NOW, read.appliedAt());
    }

    @Test
    void eachUserSeesOnlyTheirApplicationsLastMovedFirst() {
        JobOffer android = publish("Desarrollador Android", NOW);
        JobOffer ux = publish("Diseñador UX", NOW);
        JobApplication first = applications.save(JobApplication.apply(ANA.userId(), android, null, NOW));
        applications.save(JobApplication.apply(ANA.userId(), ux, null, NOW + 1));
        applications.save(JobApplication.apply(LUIS.userId(), android, null, NOW));
        applications.save(first.moveTo(ApplicationStatus.INTERVIEW, NOW + 5));

        PageResult<JobApplication> ana = applications.searchByUser(ANA.userId(), new PageQuery(1, 10, null));

        assertEquals(2, ana.total());
        assertEquals("Desarrollador Android", ana.items().get(0).jobTitle());
        assertEquals(1, applications.searchByUser(ANA.userId(), new PageQuery(1, 10, "ux")).total());
        assertEquals(1, applications.searchByUser(LUIS.userId(), new PageQuery(1, 10, null)).total());
    }

    @Test
    void withdrawingDeletesTheRow() {
        JobOffer offer = publish("Dev", NOW);
        JobApplication a = applications.save(JobApplication.apply(ANA.userId(), offer, null, NOW));

        applications.delete(a.id());

        assertTrue(applications.findById(a.id()).isEmpty());
    }
}
