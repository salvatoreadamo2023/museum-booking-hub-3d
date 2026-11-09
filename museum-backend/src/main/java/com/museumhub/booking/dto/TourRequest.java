package com.museumhub.booking.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/** Creazione e modifica di un itinerario. Gli orari sono nel formato HH:mm (es. "09:30"). */
public record TourRequest(
        @NotBlank(message = "Il titolo è obbligatorio")
        @Size(max = 150, message = "Il titolo può avere al massimo 150 caratteri")
        String titolo,

        @Size(max = 500, message = "La descrizione può avere al massimo 500 caratteri")
        String descrizione,

        @NotNull(message = "La durata è obbligatoria")
        @Min(value = 1, message = "La durata deve essere di almeno 1 minuto")
        @Max(value = 600, message = "La durata non può superare 600 minuti")
        Integer durataMin,

        @NotNull(message = "Il prezzo è obbligatorio")
        @DecimalMin(value = "0.01", message = "Il prezzo deve essere maggiore di zero")
        @Digits(integer = 6, fraction = 2, message = "Il prezzo può avere al massimo 2 decimali")
        BigDecimal prezzo,

        @NotNull(message = "La capienza è obbligatoria")
        @Min(value = 1, message = "La capienza deve essere di almeno 1 posto")
        @Max(value = 100, message = "La capienza non può superare 100 posti")
        Integer capienza,

        @NotEmpty(message = "Serve almeno una fascia oraria")
        List<@Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "Orario non valido (formato HH:mm)") String> orari) {
}
