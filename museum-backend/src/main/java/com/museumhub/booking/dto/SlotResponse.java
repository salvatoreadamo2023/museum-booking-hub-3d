package com.museumhub.booking.dto;

/** Fascia oraria con i posti ancora liberi nella data richiesta. */
public record SlotResponse(String orario, int liberi, boolean attivo) {
}
