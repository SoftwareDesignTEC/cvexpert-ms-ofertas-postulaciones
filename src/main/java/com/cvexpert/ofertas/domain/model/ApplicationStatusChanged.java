package com.cvexpert.ofertas.domain.model;

/**
 * Evento {@code application.status_changed}: un administrador movió la postulación de alguien.
 * Lo recibe Notificaciones para avisarle a la persona que se postuló.
 *
 * <p>Son los datos que el dominio tiene para el mensaje. La forma exacta del mensaje de la cola
 * la define Samuel; el adaptador de mensajería la arma a partir de aquí.
 *
 * @param userId     quien se postuló, que es quien recibe el aviso
 * @param occurredAt epoch milisegundos UTC
 */
public record ApplicationStatusChanged(
        long applicationId,
        String userId,
        long jobOfferId,
        String jobTitle,
        String company,
        ApplicationStatus previousStatus,
        ApplicationStatus newStatus,
        long occurredAt) {

    public static ApplicationStatusChanged between(JobApplication before, JobApplication after) {
        return new ApplicationStatusChanged(after.id(), after.userId(), after.jobOfferId(), after.jobTitle(),
                after.company(), before.status(), after.status(), after.updatedAt());
    }
}
