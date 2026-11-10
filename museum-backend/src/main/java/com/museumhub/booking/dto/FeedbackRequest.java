package com.museumhub.booking.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record FeedbackRequest(
        @NotNull(message = "L'itinerario è obbligatorio")
        Long tourId,

        @NotNull(message = "Il voto è obbligatorio")
        @Min(value = 1, message = "Il voto minimo è 1")
        @Max(value = 5, message = "Il voto massimo è 5")
        Integer voto,

        @NotBlank(message = "Il commento è obbligatorio")
        @Size(max = 1000, message = "Il commento può avere al massimo 1000 caratteri")
        String commento) {
}
