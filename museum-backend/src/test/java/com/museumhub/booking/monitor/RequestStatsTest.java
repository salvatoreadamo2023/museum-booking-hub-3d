package com.museumhub.booking.monitor;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Test unitario dei contatori delle richieste (senza avviare Spring). */
class RequestStatsTest {

    private static final long MS = 1_000_000L;

    @Test
    void senzaRichiesteTuttoZero() {
        RequestStats stats = new RequestStats();

        assertEquals(0, stats.getTotali());
        assertEquals(0.0, stats.getTempoMedioMs());
        assertEquals(0.0, stats.getTempoMassimoMs());
    }

    @Test
    void calcolaTotaleMediaEMassimo() {
        RequestStats stats = new RequestStats();
        stats.registra(10 * MS);
        stats.registra(30 * MS);
        stats.registra(50 * MS);
        stats.registra(10 * MS);

        assertEquals(4, stats.getTotali());
        assertEquals(25.0, stats.getTempoMedioMs(), 0.001);
        assertEquals(50.0, stats.getTempoMassimoMs(), 0.001);
    }

    @Test
    void ilMassimoNonDiminuisce() {
        RequestStats stats = new RequestStats();
        stats.registra(80 * MS);
        stats.registra(5 * MS);

        assertEquals(80.0, stats.getTempoMassimoMs(), 0.001);
    }
}
