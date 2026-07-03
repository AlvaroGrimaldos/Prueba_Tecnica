package com.acme.userauth.application.dto;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Página inmutable de resultados, agnóstica de la tecnología de persistencia.
 * <p>
 * Evita que {@code org.springframework.data.domain.Page} se filtre a través
 * de los puertos: los consumidores de la librería no dependen de Spring Data.
 *
 * @param <T>   tipo de los elementos
 * @param items elementos de la página actual
 * @param total número total de elementos que satisfacen la consulta
 * @param page  índice de página (base 0)
 * @param size  tamaño de página solicitado
 */
public record PageResult<T>(List<T> items, long total, int page, int size) {

    public PageResult {
        Objects.requireNonNull(items, "items must not be null");
        if (total < 0 || page < 0 || size < 1) {
            throw new IllegalArgumentException(
                    "invalid page result: total=%d, page=%d, size=%d".formatted(total, page, size));
        }
        items = List.copyOf(items);
    }

    /** Transforma los elementos conservando los metadatos de paginación. */
    public <R> PageResult<R> map(Function<T, R> mapper) {
        return new PageResult<>(items.stream().map(mapper).toList(), total, page, size);
    }

    /** Número total de páginas para el tamaño actual. */
    public int totalPages() {
        return (int) Math.ceilDiv(total, size);
    }
}
