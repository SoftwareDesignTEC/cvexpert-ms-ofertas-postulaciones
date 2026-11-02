package com.cvexpert.ofertas.domain.model;

/**
 * Evento {@code job.new}: se publicó una oferta que alcanza el mínimo de compatibilidad de un
 * usuario. Hay un evento por cada usuario que debe recibir el aviso.
 *
 * @param userId     quien recibe el aviso
 * @param occurredAt epoch milisegundos UTC
 */
public record NewJobMatched(
        String userId,
        long jobOfferId,
        String title,
        String company,
        String location,
        Modality modality,
        int matchScore,
        long occurredAt) {

    public static NewJobMatched of(String userId, JobOffer offer, int matchScore) {
        JobOfferDetails d = offer.details();
        return new NewJobMatched(userId, offer.id(), d.title(), d.company(), d.location(), d.modality(), matchScore,
                offer.postedAt());
    }
}
