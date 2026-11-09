package com.cvexpert.ofertas.domain.model;

/**
 * Una búsqueda que el importador le hace a la fuente externa.
 *
 * @param text   lo que se busca, por ejemplo "desarrollador junior en Costa Rica"
 * @param sector sector con que se guardan las ofertas de esta búsqueda; la fuente no lo trae
 */
public record JobSearchQuery(String text, String sector) {
}
