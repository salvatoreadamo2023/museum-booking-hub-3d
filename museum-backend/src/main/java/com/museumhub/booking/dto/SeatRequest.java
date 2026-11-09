package com.museumhub.booking.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Un posto scelto nella mappa 3D con la tariffa scelta per quel posto. */
public record SeatRequest(
        @NotNull(message = "Il numero del posto è obbligatorio")
        @Min(value = 1, message = "Il numero del posto deve essere almeno 1")
        Integer numero,

        @NotNull(message = "La tariffa è obbligatoria")
        Long tariffaId) {
}
