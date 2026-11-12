package com.museumhub.booking.dto;

import java.time.Instant;

/**
 * Fotografia dello stato del backend, limitata alle misure legate ad Architettura dei Calcolatori: core e uso
 * della CPU, memoria heap e garbage collector, thread attivi e tempi di risposta delle richieste.
 */
public record SistemaResponse(
        Instant rilevatoIl,
        Processore processore,
        Memoria memoria,
        Concorrenza concorrenza,
        Richieste richieste) {

    /** Core logici disponibili e uso della CPU da parte del backend in percentuale (null se non disponibile). */
    public record Processore(int core, Double cpuPercentuale) {
    }

    /** Heap Java (l'area dove vengono creati gli oggetti) e lavoro del garbage collector. */
    public record Memoria(long heapUsatoMb, long heapMassimoMb, long gcConteggio, long gcTempoMs) {
    }

    /** Thread della JVM attivi in questo momento. */
    public record Concorrenza(int threadAttivi) {
    }

    /** Richieste alle API dall'avvio e loro tempi di risposta. */
    public record Richieste(long totali, double tempoMedioMs, double tempoMassimoMs) {
    }
}
