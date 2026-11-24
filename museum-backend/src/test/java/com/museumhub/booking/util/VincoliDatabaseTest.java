package com.museumhub.booking.util;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLIntegrityConstraintViolationException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VincoliDatabaseTest {

    @Test
    void riconosceIlVincoloNelMessaggioDiMySql() {
        var errore = new DataIntegrityViolationException("could not execute statement",
                new SQLIntegrityConstraintViolationException(
                        "Duplicate entry '1-2026-12-01-09:30:00-5' for key 'reservation_seats.uk_seat_slot_posto'"));
        assertTrue(VincoliDatabase.violato(errore, "uk_seat_slot_posto"));
    }

    @Test
    void riconosceIlVincoloNelMessaggioDiH2() {
        var errore = new DataIntegrityViolationException(
                "Unique index or primary key violation: \"PUBLIC.UK_FEEDBACK_PRENOTAZIONE_INDEX_F ON PUBLIC.FEEDBACK\"");
        assertTrue(VincoliDatabase.violato(errore, "uk_feedback_prenotazione"));
    }

    @Test
    void unAltroErroreDiIntegritaNonVieneScambiatoPerIlPosto() {
        var errore = new DataIntegrityViolationException("could not execute statement",
                new SQLIntegrityConstraintViolationException("Column 'tariffa_id' cannot be null"));
        assertFalse(VincoliDatabase.violato(errore, "uk_seat_slot_posto"));
    }
}
