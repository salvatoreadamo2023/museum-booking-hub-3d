package com.museumhub.booking.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;
import java.util.List;

/** Nuova prenotazione: data nel formato yyyy-MM-dd, orario nel formato HH:mm. */
public record ReservationRequest(
        @NotNull(message = "L'itinerario è obbligatorio")
        Long tourId,

        @NotNull(message = "La data è obbligatoria")
        LocalDate data,

        @NotNull(message = "L'orario è obbligatorio")
        @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "Orario non valido (formato HH:mm)")
        String orario,

        @NotEmpty(message = "Seleziona almeno un posto")
        List<@Valid SeatRequest> posti) {
}
