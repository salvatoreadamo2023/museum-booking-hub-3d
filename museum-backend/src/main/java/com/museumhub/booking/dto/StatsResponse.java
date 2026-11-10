package com.museumhub.booking.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Indicatori per la dashboard. Le variazioni confrontano il periodo con quello precedente della stessa
 * durata: percentuale relativa per incasso e biglietti (null se prima era zero), punti percentuali per i posti.
 */
public record StatsResponse(
        String periodo,
        String da,
        String a,
        BigDecimal incasso,
        Double incassoVariazione,
        int biglietti,
        Double bigliettiVariazione,
        String oraDiPunta,
        int postiRiservati,
        int postiTotali,
        double postiRiservatiPercentuale,
        double postiRiservatiVariazione,
        List<Giorno> andamento,
        List<QuotaItinerario> itinerari) {

    public record Giorno(String data, BigDecimal incasso, int biglietti) {
    }

    public record QuotaItinerario(Long tourId, String titolo, int biglietti, BigDecimal incasso, double percentuale) {
    }
}
