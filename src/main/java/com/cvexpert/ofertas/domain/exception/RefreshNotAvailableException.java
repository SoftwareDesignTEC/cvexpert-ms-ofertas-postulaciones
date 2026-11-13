package com.cvexpert.ofertas.domain.exception;

/**
 * No se puede buscar ofertas nuevas ahora: el importador está apagado (falta la clave de la API)
 * o se usó hace muy poco. Será un 429 si es por la espera y un 503 si está apagado.
 */
public class RefreshNotAvailableException extends DomainException {

    private final boolean tooSoon;

    private RefreshNotAvailableException(String code, String message, boolean tooSoon) {
        super(code, message);
        this.tooSoon = tooSoon;
    }

    public static RefreshNotAvailableException disabled() {
        return new RefreshNotAvailableException("REFRESH_DISABLED", "La búsqueda de ofertas nuevas está apagada", false);
    }

    public static RefreshNotAvailableException tooSoon(long secondsLeft) {
        return new RefreshNotAvailableException("REFRESH_TOO_SOON",
                "Ya se buscaron ofertas hace poco; inténtalo en " + secondsLeft + " s", true);
    }

    public boolean tooSoon() {
        return tooSoon;
    }
}
