package com.cvexpert.ofertas.domain.exception;

/**
 * Error de una regla del negocio. Lleva el {@code code} y el {@code message} del esquema
 * {@code Error} del contrato; el adaptador web decide qué código HTTP le toca a cada subclase.
 */
public abstract class DomainException extends RuntimeException {

    private final String code;

    protected DomainException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
