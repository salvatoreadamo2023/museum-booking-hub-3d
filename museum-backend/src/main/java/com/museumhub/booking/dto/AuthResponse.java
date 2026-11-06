package com.museumhub.booking.dto;

import com.museumhub.booking.entity.Ruolo;
import com.museumhub.booking.entity.User;

/**
 * Risposta di login e registrazione: i dati dell'utente da mostrare nell'interfaccia e il token JWT, che il client
 * invia poi nell'intestazione "Authorization: Bearer".
 */
public record AuthResponse(Long id, String nome, String email, Ruolo ruolo, String token) {

    public static AuthResponse of(User user, String token) {
        return new AuthResponse(user.getId(), user.getNome(), user.getEmail(), user.getRuolo(), token);
    }
}
