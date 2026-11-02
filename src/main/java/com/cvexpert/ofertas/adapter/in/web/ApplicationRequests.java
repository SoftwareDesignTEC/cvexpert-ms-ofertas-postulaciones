package com.cvexpert.ofertas.adapter.in.web;

/** Cuerpos que reciben las rutas de postulaciones. */
final class ApplicationRequests {

    private ApplicationRequests() {
    }

    /**
     * {@code POST /applications}. Los clientes de la Fase I mandan además {@code userId},
     * {@code jobTitle}, {@code company}, etc.; Spring ignora los campos que no están aquí, así que
     * siguen funcionando sin cambios.
     */
    record Create(String jobOfferId, String notes) {
    }

    /** {@code PUT /applications/{id}} y la ruta de administración. */
    record UpdateStatus(String status) {
    }
}
