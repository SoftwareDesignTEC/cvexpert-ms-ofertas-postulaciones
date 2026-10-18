package com.cvexpert.ofertas.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Prueba del dominio sin Spring ni base de datos: corre en milisegundos. */
class ApplicationStatusTest {

    @Test
    void usesTheSameValuesAsPhaseOne() {
        List<String> wire = Arrays.stream(ApplicationStatus.values()).map(ApplicationStatus::wireValue).toList();
        assertEquals(List.of("applied", "in-progress", "interview", "rejected"), wire);
    }

    @Test
    void readsEveryCanonicalValueBack() {
        for (ApplicationStatus status : ApplicationStatus.values()) {
            assertEquals(status, ApplicationStatus.fromWireValue(status.wireValue()).orElseThrow());
        }
    }

    @Test
    void rejectsValuesOutsideTheContract() {
        assertTrue(ApplicationStatus.fromWireValue("in_progress").isEmpty());
        assertTrue(ApplicationStatus.fromWireValue("accepted").isEmpty());
        assertTrue(ApplicationStatus.fromWireValue(null).isEmpty());
    }
}
