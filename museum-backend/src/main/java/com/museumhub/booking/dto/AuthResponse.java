package com.museumhub.booking.dto;

import com.museumhub.booking.entity.Ruolo;
import com.museumhub.booking.entity.User;

/**
 * Risposta di login, registrazione, refresh e /auth/me: i dati dell'utente da mostrare nell'interfaccia.
 * Il token NON e' nel corpo: viaggia solo nei cookie HttpOnly, quindi JavaScript non puo' leggerlo.
 */
public record AuthResponse(Long id, String nome, String email, Ruolo ruolo) {

    public static AuthResponse of(User user) {
        return new AuthResponse(user.getId(), user.getNome(), user.getEmail(), user.getRuolo());
    }
}
