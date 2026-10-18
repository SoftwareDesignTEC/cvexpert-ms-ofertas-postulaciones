package com.cvexpert.ofertas.domain.model;

import java.util.Arrays;
import java.util.Optional;

/**
 * Estado de una postulación: las cuatro columnas del tablero Kanban.
 *
 * <p>El valor que viaja en la API y en los eventos ({@link #wireValue()}) es el mismo de la
 * Fase I ({@code docs/contract/models.ts} de los clientes), así la web y la app no cambian.
 */
public enum ApplicationStatus {
    APPLIED("applied"),
    IN_PROGRESS("in-progress"),
    INTERVIEW("interview"),
    REJECTED("rejected");

    private final String wireValue;

    ApplicationStatus(String wireValue) {
        this.wireValue = wireValue;
    }

    /** Texto con el que el estado viaja en JSON: "applied", "in-progress", etc. */
    public String wireValue() {
        return wireValue;
    }

    /**
     * El estado que corresponde a un texto de la API, o vacío si no es ninguno de los cuatro.
     *
     * <p>Solo acepta los valores canónicos. Los mocks de la Fase I mandaban también
     * {@code in_progress}; el servicio real no los acepta, para que el contrato tenga una sola
     * forma.
     */
    public static Optional<ApplicationStatus> fromWireValue(String value) {
        return Arrays.stream(values()).filter(s -> s.wireValue.equals(value)).findFirst();
    }
}
