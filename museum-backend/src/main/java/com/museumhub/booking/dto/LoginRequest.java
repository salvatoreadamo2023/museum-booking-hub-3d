package com.museumhub.booking.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "L'email è obbligatoria")
        @Email(message = "Formato email non valido")
        @Size(max = 150, message = "L'email può avere al massimo 150 caratteri")
        String email,

        @NotBlank(message = "La password è obbligatoria")
        @Size(max = 72, message = "La password può avere al massimo 72 caratteri")
        @PasswordBcrypt
        String password) {
}
