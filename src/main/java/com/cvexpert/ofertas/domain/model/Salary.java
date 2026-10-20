package com.cvexpert.ofertas.domain.model;

import com.cvexpert.ofertas.domain.exception.InvalidRequestException;
import java.math.BigDecimal;

/** Rango salarial de una oferta. Va completo (mínimo, máximo y moneda) o no va. */
public record Salary(BigDecimal min, BigDecimal max, String currency) {

    public Salary {
        if (min == null || max == null || currency == null || currency.isBlank()) {
            throw new InvalidRequestException("El salario necesita mínimo, máximo y moneda");
        }
        if (min.signum() < 0 || min.compareTo(max) > 0) {
            throw new InvalidRequestException("El salario mínimo tiene que estar entre 0 y el máximo");
        }
    }
}
