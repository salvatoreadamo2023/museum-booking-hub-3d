package com.museumhub.booking.dto;

import com.museumhub.booking.entity.Ruolo;
import com.museumhub.booking.entity.StatoAccount;

/** Dati pubblici di un utente: non contiene mai la password. */
public record UserResponse(Long id, String nome, String email, Ruolo ruolo, StatoAccount stato, String creatoIl) {
}
