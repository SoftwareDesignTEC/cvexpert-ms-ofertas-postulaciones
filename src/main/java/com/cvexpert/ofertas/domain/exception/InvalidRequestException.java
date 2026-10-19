package com.cvexpert.ofertas.domain.exception;

/** Datos que no cumplen el contrato: falta un campo, un valor fuera de rango. Será un 400. */
public class InvalidRequestException extends DomainException {

    public InvalidRequestException(String message) {
        super("VALIDATION_ERROR", message);
    }
}
