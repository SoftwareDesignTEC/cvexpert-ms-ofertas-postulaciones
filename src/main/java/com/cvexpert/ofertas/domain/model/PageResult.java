package com.cvexpert.ofertas.domain.model;

import java.util.List;
import java.util.function.Function;

/**
 * Una página de resultados con la forma de los listados del contrato.
 *
 * @param total cantidad total de elementos que cumplen la búsqueda, no de páginas
 */
public record PageResult<T>(List<T> items, int page, int pageSize, long total) {

    public PageResult {
        items = List.copyOf(items);
    }

    public <R> PageResult<R> map(Function<T, R> mapper) {
        return new PageResult<>(items.stream().map(mapper).toList(), page, pageSize, total);
    }
}
