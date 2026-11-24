package com.museumhub.booking.util;

import org.springframework.dao.DataIntegrityViolationException;

import java.util.Locale;

/**
 * Riconosce quale vincolo del database ha bloccato un salvataggio. Serve a rispondere "posto già occupato" o
 * "feedback già lasciato" solo quando è davvero quel vincolo, senza nascondere altri errori di integrità.
 * Il nome del vincolo compare nel messaggio sia di MySQL ("for key 'reservation_seats.uk_seat_slot_posto'")
 * sia di H2 ("UK_SEAT_SLOT_POSTO_INDEX_..."), per questo il confronto ignora maiuscole e minuscole.
 */
public final class VincoliDatabase {

    private VincoliDatabase() {
    }

    public static boolean violato(DataIntegrityViolationException errore, String nomeVincolo) {
        String cercato = nomeVincolo.toLowerCase(Locale.ROOT);
        Throwable causa = errore;
        int giri = 0;
        while (causa != null && giri++ < 10) {
            String messaggio = causa.getMessage();
            if (messaggio != null && messaggio.toLowerCase(Locale.ROOT).contains(cercato)) {
                return true;
            }
            causa = causa.getCause();
        }
        return false;
    }
}
