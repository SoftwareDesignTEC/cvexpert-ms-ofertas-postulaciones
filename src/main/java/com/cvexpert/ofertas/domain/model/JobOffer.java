package com.cvexpert.ofertas.domain.model;

/**
 * Una oferta de trabajo publicada.
 *
 * @param id       {@code null} mientras no se ha guardado; lo asigna la base
 * @param postedAt fecha de publicación en epoch milisegundos UTC
 */
public record JobOffer(Long id, JobOfferDetails details, long postedAt) {

    /** Una oferta nueva, todavía sin id. */
    public static JobOffer publish(JobOfferDetails details, long now) {
        return new JobOffer(null, details, now);
    }

    /** La misma oferta con otra descripción. El id y la fecha de publicación no cambian. */
    public JobOffer withDetails(JobOfferDetails newDetails) {
        return new JobOffer(id, newDetails, postedAt);
    }
}
