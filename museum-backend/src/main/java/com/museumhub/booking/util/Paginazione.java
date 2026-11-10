package com.museumhub.booking.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

/** Porta nei limiti i parametri di paginazione ricevuti dal client, uguale per tutti gli elenchi. */
public final class Paginazione {

    public static final int DIMENSIONE_MASSIMA = 100;

    private Paginazione() {
    }

    /** Pagina minima 0 (la prima), dimensione tra 1 e {@value #DIMENSIONE_MASSIMA}. */
    public static PageRequest richiesta(int pagina, int dimensione, Sort ordine) {
        return PageRequest.of(Math.max(0, pagina), Math.min(DIMENSIONE_MASSIMA, Math.max(1, dimensione)), ordine);
    }
}
