package com.cvexpert.ofertas.domain.exception;

/** La operación es de administración y quien llama no tiene ese rol. Será un 403. */
public class ForbiddenException extends DomainException {

    public ForbiddenException() {
        super("FORBIDDEN", "Esta operación es solo para administradores");
    }
}
