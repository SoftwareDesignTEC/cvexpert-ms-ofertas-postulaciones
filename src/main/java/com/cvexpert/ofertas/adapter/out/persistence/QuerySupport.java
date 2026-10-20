package com.cvexpert.ofertas.adapter.out.persistence;

import com.cvexpert.ofertas.domain.model.PageQuery;
import com.cvexpert.ofertas.domain.model.PageResult;
import java.util.Locale;
import java.util.function.Function;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

/** Ayudas compartidas por los dos adaptadores: búsqueda con LIKE y paginación. */
final class QuerySupport {

    private QuerySupport() {
    }

    /**
     * Patrón LIKE que busca {@code text} en cualquier parte, sin distinguir mayúsculas. Escapa
     * {@code %} y {@code _} para que quien busca "100%" no reciba todo.
     */
    static String contains(String text) {
        String escaped = text.toLowerCase(Locale.ROOT)
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");
        return "%" + escaped + "%";
    }

    /** La página de Spring Data que corresponde a {@code query} (Spring cuenta desde 0). */
    static PageRequest pageRequest(PageQuery query, Sort sort) {
        return PageRequest.of(query.page() - 1, query.pageSize(), sort);
    }

    static <E, T> PageResult<T> toResult(Page<E> page, PageQuery query, Function<E, T> mapper) {
        return new PageResult<>(page.getContent().stream().map(mapper).toList(), query.page(), query.pageSize(),
                page.getTotalElements());
    }
}
