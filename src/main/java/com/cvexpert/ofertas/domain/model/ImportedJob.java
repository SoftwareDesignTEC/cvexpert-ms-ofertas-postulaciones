package com.cvexpert.ofertas.domain.model;

/**
 * Una oferta real traída de una fuente externa, lista para guardarse.
 *
 * @param source     nombre de la fuente ("jsearch")
 * @param externalId id de la oferta en esa fuente; con {@code source} evita duplicarla
 * @param details    la oferta ya convertida a nuestro modelo (incluye {@code applyUrl})
 * @param postedAt   fecha de publicación en la fuente, en epoch milisegundos UTC
 */
public record ImportedJob(String source, String externalId, JobOfferDetails details, long postedAt) {
}
