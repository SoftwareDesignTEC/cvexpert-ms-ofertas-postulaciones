package com.cvexpert.ofertas.domain.model;

/**
 * Una oferta con su compatibilidad para quien consulta.
 *
 * @param matchScore 0 a 100, o {@code null} si quien consulta no ha guardado su perfil de
 *                   emparejamiento (el contrato dice que entonces el campo falta)
 */
public record ScoredJobOffer(JobOffer offer, Integer matchScore) {
}
