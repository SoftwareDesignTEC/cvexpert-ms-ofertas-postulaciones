package com.cvexpert.ofertas.domain.model;

import com.cvexpert.ofertas.domain.exception.InvalidRequestException;

/**
 * Qué página pedir y qué buscar, con los límites del contrato: {@code page} desde 1 y
 * {@code pageSize} entre 1 y 100.
 *
 * @param search texto a buscar, o {@code null} para no filtrar. Un texto en blanco cuenta como
 *               {@code null}.
 */
public record PageQuery(int page, int pageSize, String search) {

    public static final int MAX_PAGE_SIZE = 100;

    public PageQuery {
        if (page < 1) {
            throw new InvalidRequestException("page empieza en 1");
        }
        if (pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new InvalidRequestException("pageSize tiene que estar entre 1 y " + MAX_PAGE_SIZE);
        }
        search = search == null || search.isBlank() ? null : search.strip();
    }

    public boolean hasSearch() {
        return search != null;
    }
}
