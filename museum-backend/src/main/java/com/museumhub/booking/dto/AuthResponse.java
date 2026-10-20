package com.museumhub.booking.dto;

import com.museumhub.booking.entity.Ruolo;

/** Risposta di login e registrazione: il token JWT e i dati da mostrare nella sidebar. */
public record AuthResponse(String token, Long id, String nome, String email, Ruolo ruolo) {
}
