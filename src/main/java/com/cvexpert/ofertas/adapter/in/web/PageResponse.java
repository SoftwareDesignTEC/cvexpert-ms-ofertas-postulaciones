package com.cvexpert.ofertas.adapter.in.web;

import com.cvexpert.ofertas.domain.model.PageResult;
import java.util.List;
import java.util.function.Function;

/** Forma de los listados del contrato: {@code {items, page, pageSize, total}}. */
public record PageResponse<T>(List<T> items, int page, int pageSize, long total) {

    static <D, T> PageResponse<T> of(PageResult<D> result, Function<D, T> mapper) {
        return new PageResponse<>(result.items().stream().map(mapper).toList(), result.page(), result.pageSize(),
                result.total());
    }
}
