package com.museumhub.booking.security;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Conta gli eventi di ogni chiave (un IP, una coppia IP + email...) dentro una finestra di tempo fissa.
 * La mappa ha una dimensione massima vera: quando e' piena butta via la chiave usata meno di recente,
 * cosi' chi inventa milioni di chiavi diverse non puo' far crescere la memoria all'infinito.
 * I metodi sono synchronized: le operazioni sono brevissime e il traffico di login e' basso.
 */
final class LimitatoreAFinestra {

    private static final class Finestra {
        private Instant inizio;
        private int conteggio;
    }

    private final Duration durata;
    private final Map<String, Finestra> finestre;

    LimitatoreAFinestra(int massimoChiavi, Duration durata) {
        this.durata = durata;
        // accessOrder = true: l'ordine segue l'ultimo utilizzo, la prima chiave e' quella dimenticata da piu' tempo
        this.finestre = new LinkedHashMap<>(256, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Finestra> piuVecchia) {
                return size() > massimoChiavi;
            }
        };
    }

    /** Quanti eventi ha la chiave nella finestra in corso (0 se la finestra e' scaduta o la chiave e' nuova). */
    synchronized int conteggio(String chiave, Instant ora) {
        Finestra f = finestre.get(chiave);
        if (f == null) {
            return 0;
        }
        if (scaduta(f, ora)) {
            finestre.remove(chiave);
            return 0;
        }
        return f.conteggio;
    }

    /** Registra un evento e restituisce il nuovo conteggio della finestra. */
    synchronized int incrementa(String chiave, Instant ora) {
        Finestra f = finestre.get(chiave);
        if (f == null || scaduta(f, ora)) {
            f = new Finestra();
            f.inizio = ora;
            finestre.put(chiave, f);
        }
        return ++f.conteggio;
    }

    /**
     * Controllo e conteggio in un solo passo atomico: se la chiave e' sotto il massimo registra l'evento e
     * restituisce true, altrimenti non tocca nulla e restituisce false. Con due chiamate separate (prima
     * leggo, poi incremento) due richieste simultanee potrebbero leggere lo stesso valore e superare la soglia.
     */
    synchronized boolean consumaSeSotto(String chiave, Instant ora, int massimo) {
        if (conteggio(chiave, ora) >= massimo) {
            return false;
        }
        incrementa(chiave, ora);
        return true;
    }

    /** Annulla un evento registrato prima (per esempio un tentativo di login che poi e' andato a buon fine). */
    synchronized void restituisci(String chiave, Instant ora) {
        Finestra f = finestre.get(chiave);
        if (f != null && !scaduta(f, ora) && f.conteggio > 0) {
            f.conteggio--;
        }
    }

    /** Minuti che mancano alla fine della finestra della chiave, arrotondati per eccesso (almeno 1). */
    synchronized long minutiAllaFine(String chiave, Instant ora) {
        Finestra f = finestre.get(chiave);
        if (f == null) {
            return 1;
        }
        long secondi = Duration.between(ora, f.inizio.plus(durata)).toSeconds();
        return Math.max(1, (secondi + 59) / 60);
    }

    synchronized void azzera(String chiave) {
        finestre.remove(chiave);
    }

    synchronized int dimensione() {
        return finestre.size();
    }

    private boolean scaduta(Finestra f, Instant ora) {
        return !ora.isBefore(f.inizio.plus(durata));
    }
}
