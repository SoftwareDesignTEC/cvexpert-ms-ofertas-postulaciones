package com.cvexpert.ofertas.adapter.in.web;

import com.cvexpert.ofertas.domain.model.PageResult;
import java.util.List;
import java.util.function.Function;

/**
 * Forma de los listados del contrato: {@code {items, page, pageSize, totalRecords}}. Se llama
 * {@code totalRecords} y no {@code total} porque así lo pide el enunciado y así lo leen la web y
 * Android (ver {@code PagedResult} en el contrato de la web).
 */
public record PageResponse<T>(List<T> items, int page, int pageSize, long totalRecords) {

    static <D, T> PageResponse<T> of(PageResult<D> result, Function<D, T> mapper) {
        return new PageResponse<>(result.items().stream().map(mapper).toList(), result.page(), result.pageSize(),
                result.total());
    }
}
