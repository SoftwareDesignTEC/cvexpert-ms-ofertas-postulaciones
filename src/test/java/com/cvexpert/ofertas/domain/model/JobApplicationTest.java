package com.cvexpert.ofertas.domain.model;

import static com.cvexpert.ofertas.Fixtures.NOW;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cvexpert.ofertas.Fixtures;
import org.junit.jupiter.api.Test;

class JobApplicationTest {

    private final JobOffer offer = new JobOffer(7L, Fixtures.details("Desarrollador Android"), NOW - 1000);

    @Test
    void applyingCopiesTheOfferAndStartsInApplied() {
        JobApplication a = JobApplication.apply("sub-1", offer, "Me interesa", NOW);

        assertNull(a.id());
        assertEquals(7L, a.jobOfferId());
        assertEquals("Desarrollador Android", a.jobTitle());
        assertEquals("Acme CR", a.company());
        assertEquals("San José", a.location());
        assertEquals(Modality.HYBRID, a.modality());
        assertEquals(ApplicationStatus.APPLIED, a.status());
        assertEquals(NOW, a.appliedAt());
        assertEquals(NOW, a.updatedAt());
        assertTrue(a.belongsTo("sub-1"));
        assertFalse(a.belongsTo("sub-2"));
    }

    @Test
    void movingChangesStatusAndUpdatedAtOnly() {
        JobApplication a = JobApplication.apply("sub-1", offer, null, NOW);
        JobApplication moved = a.moveTo(ApplicationStatus.INTERVIEW, NOW + 5000);

        assertEquals(ApplicationStatus.INTERVIEW, moved.status());
        assertEquals(NOW + 5000, moved.updatedAt());
        assertEquals(NOW, moved.appliedAt());
        assertEquals(a.jobTitle(), moved.jobTitle());
    }

    @Test
    void movingToTheSameStatusIsNotAChange() {
        JobApplication a = JobApplication.apply("sub-1", offer, null, NOW);
        assertSame(a, a.moveTo(ApplicationStatus.APPLIED, NOW + 5000));
    }

    @Test
    void theEventCarriesBothStatuses() {
        JobApplication before = new JobApplication(3L, "sub-1", 7L, "Dev", "Acme", null, "Cartago",
                Modality.REMOTE, ApplicationStatus.APPLIED, NOW, NOW, null);
        JobApplication after = before.moveTo(ApplicationStatus.REJECTED, NOW + 1);

        ApplicationStatusChanged event = ApplicationStatusChanged.between(before, after);

        assertEquals(3L, event.applicationId());
        assertEquals("sub-1", event.userId());
        assertEquals(ApplicationStatus.APPLIED, event.previousStatus());
        assertEquals(ApplicationStatus.REJECTED, event.newStatus());
        assertEquals(NOW + 1, event.occurredAt());
    }
}
