package com.museumhub.booking.monitor;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * Contatori delle richieste alle API dall'avvio del backend: quante sono e quanto sono durate.
 * I contatori reggono l'accesso da piu' thread senza bisogno di blocchi. Valgono per la singola istanza
 * e si azzerano al riavvio.
 */
@Component
public class RequestStats {

    private static final double NS_PER_MS = 1_000_000.0;

    private final LongAdder totali = new LongAdder();
    private final LongAdder sommaNs = new LongAdder();
    private final AtomicLong massimoNs = new AtomicLong();

    public void registra(long durataNs) {
        totali.increment();
        sommaNs.add(durataNs);
        massimoNs.accumulateAndGet(durataNs, Math::max);
    }

    public long getTotali() {
        return totali.sum();
    }

    public double getTempoMedioMs() {
        long n = totali.sum();
        return n == 0 ? 0 : sommaNs.sum() / (double) n / NS_PER_MS;
    }

    public double getTempoMassimoMs() {
        return massimoNs.get() / NS_PER_MS;
    }
}
