package com.museumhub.booking.dto;

import java.math.BigDecimal;

/** Tariffa con il prezzo gia' calcolato per l'itinerario. */
public record TariffaResponse(Long id, String nome, int scontoPercentuale, BigDecimal prezzo, boolean predefinita) {
}
