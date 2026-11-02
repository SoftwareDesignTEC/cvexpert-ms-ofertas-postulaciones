package com.cvexpert.ofertas.domain.model;

/**
 * Cuánto pesa cada criterio en la compatibilidad. No tienen que sumar 100: el puntaje se
 * normaliza con la suma, así que (40, 20, 20, 20) y (2, 1, 1, 1) dan lo mismo.
 */
public record MatchWeights(int skills, int location, int availability, int experience) {

    /** Los pesos propuestos para la Fase II; se pueden cambiar en {@code application.yml}. */
    public static final MatchWeights DEFAULT = new MatchWeights(40, 20, 20, 20);

    public MatchWeights {
        if (skills < 0 || location < 0 || availability < 0 || experience < 0) {
            throw new IllegalArgumentException("Los pesos de compatibilidad no pueden ser negativos");
        }
        if (skills + location + availability + experience == 0) {
            throw new IllegalArgumentException("Al menos un peso de compatibilidad tiene que ser mayor que 0");
        }
    }

    int total() {
        return skills + location + availability + experience;
    }
}
