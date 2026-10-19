package com.cvexpert.ofertas.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cvexpert.ofertas.domain.exception.InvalidRequestException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class JobOfferDetailsTest {

    @Test
    void missingListsBecomeEmpty() {
        JobOfferDetails d = new JobOfferDetails("Pasantía QA", "Acme", null, "Cartago", Modality.ON_SITE,
                JobType.INTERNSHIP, "Tecnología", "Pruebas manuales", null, null, null, null);
        assertTrue(d.requirements().isEmpty());
        assertTrue(d.skills().isEmpty());
    }

    @Test
    void copiesListsSoLaterChangesDoNotLeakIn() {
        List<String> skills = new ArrayList<>(List.of("Kotlin"));
        JobOfferDetails d = new JobOfferDetails("Dev", "Acme", null, "Cartago", Modality.ON_SITE,
                JobType.FULL_TIME, "Tecnología", "Desc", List.of(), skills, null, null);
        skills.add("Java");
        assertEquals(List.of("Kotlin"), d.skills());
    }

    @Test
    void rejectsBlankRequiredFields() {
        InvalidRequestException e = assertThrows(InvalidRequestException.class,
                () -> new JobOfferDetails(" ", "Acme", null, "Cartago", Modality.ON_SITE, JobType.FULL_TIME,
                        "Tecnología", "Desc", null, null, null, null));
        assertEquals("VALIDATION_ERROR", e.code());
        assertThrows(InvalidRequestException.class,
                () -> new JobOfferDetails("Dev", "Acme", null, "Cartago", null, JobType.FULL_TIME,
                        "Tecnología", "Desc", null, null, null, null));
    }

    @Test
    void rejectsNegativeExperience() {
        assertThrows(InvalidRequestException.class,
                () -> new JobOfferDetails("Dev", "Acme", null, "Cartago", Modality.ON_SITE, JobType.FULL_TIME,
                        "Tecnología", "Desc", null, null, -1, null));
    }

    @Test
    void salaryNeedsAValidRange() {
        assertThrows(InvalidRequestException.class,
                () -> new Salary(new BigDecimal("900"), new BigDecimal("600"), "CRC"));
        assertThrows(InvalidRequestException.class,
                () -> new Salary(new BigDecimal("-1"), new BigDecimal("600"), "CRC"));
        assertThrows(InvalidRequestException.class, () -> new Salary(BigDecimal.ONE, BigDecimal.TEN, " "));
    }

    @Test
    void enumsUseTheWireValuesOfPhaseOne() {
        assertEquals("on-site", Modality.ON_SITE.wireValue());
        assertEquals(Modality.HYBRID, Modality.fromWireValue("hybrid").orElseThrow());
        assertEquals("part-time", JobType.PART_TIME.wireValue());
        assertTrue(JobType.fromWireValue("part_time").isEmpty());
    }
}
