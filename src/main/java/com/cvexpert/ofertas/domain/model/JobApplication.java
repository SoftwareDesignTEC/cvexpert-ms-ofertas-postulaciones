package com.cvexpert.ofertas.domain.model;

/**
 * La postulación de un usuario a una oferta: una tarjeta del tablero Kanban.
 *
 * <p>Guarda una copia del título, la empresa, el logo, la ubicación y la modalidad de la oferta,
 * así el tablero se sigue viendo igual aunque la oferta se edite o se borre.
 *
 * @param id        {@code null} mientras no se ha guardado
 * @param userId    {@code sub} de Google de quien se postuló
 * @param appliedAt fecha de la postulación, epoch milisegundos UTC
 * @param updatedAt último cambio, epoch milisegundos UTC
 */
public record JobApplication(
        Long id,
        String userId,
        long jobOfferId,
        String jobTitle,
        String company,
        String companyLogoUrl,
        String location,
        Modality modality,
        ApplicationStatus status,
        long appliedAt,
        long updatedAt,
        String notes) {

    /** Una postulación nueva a {@code offer}. Nace en {@link ApplicationStatus#APPLIED}. */
    public static JobApplication apply(String userId, JobOffer offer, String notes, long now) {
        JobOfferDetails d = offer.details();
        return new JobApplication(null, userId, offer.id(), d.title(), d.company(), d.companyLogoUrl(),
                d.location(), d.modality(), ApplicationStatus.APPLIED, now, now, notes);
    }

    public boolean belongsTo(String otherUserId) {
        return userId.equals(otherUserId);
    }

    /**
     * La postulación en el estado {@code newStatus}. Si ya estaba en ese estado devuelve la misma
     * instancia sin tocar {@code updatedAt}; así quien llama sabe que no hubo cambio y que no hay
     * evento que publicar.
     */
    public JobApplication moveTo(ApplicationStatus newStatus, long now) {
        if (newStatus == status) {
            return this;
        }
        return new JobApplication(id, userId, jobOfferId, jobTitle, company, companyLogoUrl, location,
                modality, newStatus, appliedAt, now, notes);
    }
}
