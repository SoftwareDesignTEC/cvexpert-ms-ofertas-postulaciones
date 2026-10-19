package com.cvexpert.ofertas.domain.model;

import java.util.Arrays;
import java.util.Optional;

/** Modalidad de trabajo de una oferta. Los valores son los de la Fase I. */
public enum Modality {
    REMOTE("remote"),
    ON_SITE("on-site"),
    HYBRID("hybrid");

    private final String wireValue;

    Modality(String wireValue) {
        this.wireValue = wireValue;
    }

    /** Texto con el que la modalidad viaja en JSON y se guarda en la base. */
    public String wireValue() {
        return wireValue;
    }

    /** La modalidad que corresponde a un texto, o vacío si no es ninguna. */
    public static Optional<Modality> fromWireValue(String value) {
        return Arrays.stream(values()).filter(m -> m.wireValue.equals(value)).findFirst();
    }
}
