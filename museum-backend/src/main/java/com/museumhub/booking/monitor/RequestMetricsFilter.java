package com.museumhub.booking.monitor;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Misura il tempo di ogni richiesta alle API (dall'ingresso nel filtro alla risposta, sicurezza compresa) e lo
 * registra in {@link RequestStats}. Restano fuori le richieste di verifica del browser (OPTIONS) e quelle della pagina
 * di monitoraggio stessa, che altrimenti falserebbero i numeri con il proprio aggiornamento periodico.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@RequiredArgsConstructor
public class RequestMetricsFilter extends OncePerRequestFilter {

    private static final String PERCORSO_MONITORAGGIO = "/api/admin/sistema";

    private final RequestStats stats;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return !uri.startsWith("/api/")
                || uri.equals(PERCORSO_MONITORAGGIO)
                || "OPTIONS".equalsIgnoreCase(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long inizio = System.nanoTime();
        try {
            chain.doFilter(request, response);
        } finally {
            stats.registra(System.nanoTime() - inizio);
        }
    }
}
