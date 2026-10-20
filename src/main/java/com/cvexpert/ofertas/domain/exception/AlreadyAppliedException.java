package com.cvexpert.ofertas.domain.exception;

/** El usuario ya tiene una postulación a esa oferta. Será un 409. */
public class AlreadyAppliedException extends DomainException {

    public AlreadyAppliedException() {
        super("APPLICATION_ALREADY_EXISTS", "Ya te postulaste a esta oferta");
    }
}
