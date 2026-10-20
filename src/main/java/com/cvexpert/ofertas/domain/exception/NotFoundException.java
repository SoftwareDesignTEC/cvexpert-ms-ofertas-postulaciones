package com.cvexpert.ofertas.domain.exception;

/**
 * No existe, o existe pero es de otro usuario. Las dos cosas responden igual (404) para no
 * revelar qué ids de postulaciones ajenas existen.
 */
public class NotFoundException extends DomainException {

    public NotFoundException(String code, String message) {
        super(code, message);
    }

    public static NotFoundException jobOffer(long id) {
        return new NotFoundException("JOB_NOT_FOUND", "No existe la oferta " + id);
    }

    public static NotFoundException application(long id) {
        return new NotFoundException("APPLICATION_NOT_FOUND", "No existe la postulación " + id);
    }
}
