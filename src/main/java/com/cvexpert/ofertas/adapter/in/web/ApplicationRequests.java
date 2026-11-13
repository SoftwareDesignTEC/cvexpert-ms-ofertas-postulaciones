package com.cvexpert.ofertas.adapter.in.web;

/** Cuerpos que reciben las rutas de postulaciones. */
final class ApplicationRequests {

    private ApplicationRequests() {
    }

    /**
     * {@code POST /applications}. Los clientes de la Fase I mandan además {@code userId},
     * {@code jobTitle}, {@code company}, etc.; Spring ignora los campos que no están aquí, así que
     * siguen funcionando sin cambios.
     *
     * <p>El {@code status} que mandan los clientes de la Fase I se sigue ignorando (siempre nace
     * en {@code applied}). Para elegir la columna se usa un campo aparte, así nadie que ya mande
     * {@code status} cambia de comportamiento.
     *
     * @param initialStatus columna donde se guarda (opcional; sin valor, {@code applied}). Así
     *                      la app puede guardar una oferta directo en "Me interesa" o en
     *                      "Entrevista".
     */
    record Create(String jobOfferId, String notes, String initialStatus) {
    }

    /** {@code PUT /applications/{id}} y la ruta de administración. */
    record UpdateStatus(String status) {
    }
}
