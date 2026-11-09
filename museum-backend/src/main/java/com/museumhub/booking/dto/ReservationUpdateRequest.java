package com.museumhub.booking.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** Modifica da parte dell'admin: nuova fascia oraria e numero totale di posti della prenotazione. */
public record ReservationUpdateRequest(
        @NotNull(message = "L'orario è obbligatorio")
        @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "Orario non valido (formato HH:mm)")
        String orario,

        @NotNull(message = "Il numero di posti è obbligatorio")
        @Min(value = 1, message = "Serve almeno un posto")
        Integer numeroPosti) {
}
