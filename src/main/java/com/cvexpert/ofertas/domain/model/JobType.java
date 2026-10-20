package com.cvexpert.ofertas.domain.model;

import java.util.Arrays;
import java.util.Optional;

/** Tipo de jornada de una oferta (la "disponibilidad" del perfil). Valores de la Fase I. */
public enum JobType {
    FULL_TIME("full-time"),
    PART_TIME("part-time"),
    FREELANCE("freelance"),
    INTERNSHIP("internship");

    private final String wireValue;

    JobType(String wireValue) {
        this.wireValue = wireValue;
    }

    /** Texto con el que el tipo viaja en JSON y se guarda en la base. */
    public String wireValue() {
        return wireValue;
    }

    /** El tipo que corresponde a un texto, o vacío si no es ninguno. */
    public static Optional<JobType> fromWireValue(String value) {
        return Arrays.stream(values()).filter(t -> t.wireValue.equals(value)).findFirst();
    }
}
