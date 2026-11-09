package com.museumhub.booking.dto;

import java.math.BigDecimal;
import java.util.List;

public record TourResponse(
        Long id,
        String titolo,
        String descrizione,
        Integer durataMin,
        BigDecimal prezzo,
        Integer capienza,
        List<SlotResponse> orari,
        List<TariffaResponse> tariffe) {
}
