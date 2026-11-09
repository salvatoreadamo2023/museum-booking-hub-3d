package com.museumhub.booking.dto;

import com.museumhub.booking.entity.Ruolo;
import com.museumhub.booking.entity.StatoAccount;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Creazione di un utente da parte dell'admin. Se lo stato manca, l'account e' ATTIVO. */
public record UserCreateRequest(
        @NotBlank(message = "Il nome è obbligatorio")
        @Size(max = 100, message = "Il nome può avere al massimo 100 caratteri")
        String nome,

        @NotBlank(message = "L'email è obbligatoria")
        @Email(message = "Formato email non valido")
        @Size(max = 150, message = "L'email può avere al massimo 150 caratteri")
        String email,

        @NotBlank(message = "La password è obbligatoria")
        @Size(min = 8, max = 72, message = "La password deve avere tra 8 e 72 caratteri")
        String password,

        @NotNull(message = "Il ruolo è obbligatorio")
        Ruolo ruolo,

        StatoAccount stato) {
}
