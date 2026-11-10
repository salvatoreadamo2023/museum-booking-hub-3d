package com.museumhub.booking.dto;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/** Una pagina di risultati con le informazioni per scorrere l'elenco. La prima pagina e' la numero 0. */
public record PaginaResponse<T>(List<T> contenuto, int pagina, int dimensione, long totaleElementi, int totalePagine) {

    public static <E, T> PaginaResponse<T> di(Page<E> pagina, Function<E, T> conversione) {
        return new PaginaResponse<>(
                pagina.getContent().stream().map(conversione).toList(),
                pagina.getNumber(),
                pagina.getSize(),
                pagina.getTotalElements(),
                pagina.getTotalPages());
    }
}
