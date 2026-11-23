package com.museumhub.booking.security;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Richieste in parallelo sui limitatori: tanti thread partono insieme (stesso "via") per cercare di
 * infilarsi tra il controllo della soglia e il conteggio.
 */
class AccessoConcorrenteTest {

    private static final int THREAD = 16;

    /** Esegue il compito su THREAD thread, tutti allo stesso via, e conta quante volte restituisce true. */
    private int inParallelo(int ripetizioni, Callable<Boolean> compito) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(THREAD);
        CountDownLatch via = new CountDownLatch(1);
        AtomicInteger riusciti = new AtomicInteger();
        List<Future<?>> lavori = new ArrayList<>();
        try {
            for (int i = 0; i < ripetizioni; i++) {
                lavori.add(pool.submit(() -> {
                    via.await();
                    if (compito.call()) {
                        riusciti.incrementAndGet();
                    }
                    return null;
                }));
            }
            via.countDown();
            for (Future<?> lavoro : lavori) {
                lavoro.get(30, TimeUnit.SECONDS); // rilancia qui un'eventuale eccezione di un thread
            }
        } finally {
            pool.shutdownNow();
        }
        return riusciti.get();
    }

    @Test
    void registrazioniSimultaneeNonSuperanoLaSoglia() throws Exception {
        RegistrationLimitService servizio = new RegistrationLimitService(10, Duration.ofMinutes(60), new OrologioFinto());
        int passate = inParallelo(200, () -> {
            try {
                servizio.consuma("203.0.113.7");
                return true;
            } catch (RuntimeException e) {
                return false;
            }
        });
        assertEquals(10, passate);
    }

    @Test
    void loginSimultaneiSullaStessaEmailNonSuperanoICinqueTentativi() throws Exception {
        LoginAttemptService servizio = new LoginAttemptService(5, 30, Duration.ofMinutes(15), new OrologioFinto());
        int passati = inParallelo(200, () -> {
            try {
                servizio.prenota("203.0.113.7", "mario@test.it");
                return true;
            } catch (RuntimeException e) {
                return false;
            }
        });
        assertEquals(5, passati);
    }

    @Test
    void loginSimultaneiSuEmailDiverseNonSuperanoIlTettoPerIp() throws Exception {
        LoginAttemptService servizio = new LoginAttemptService(5, 30, Duration.ofMinutes(15), new OrologioFinto());
        AtomicInteger numero = new AtomicInteger();
        int passati = inParallelo(300, () -> {
            try {
                servizio.prenota("203.0.113.7", "utente" + numero.incrementAndGet() + "@test.it");
                return true;
            } catch (RuntimeException e) {
                return false;
            }
        });
        assertEquals(30, passati);
    }

    @Test
    void laMappaLruRestaCoerenteConInserimentiEdEvictionInParallelo() throws Exception {
        LimitatoreAFinestra limitatore = new LimitatoreAFinestra(1_000, Duration.ofMinutes(15));
        Instant ora = Instant.parse("2026-11-20T10:00:00Z");
        AtomicInteger numero = new AtomicInteger();
        // 50.000 chiavi diverse da 16 thread: eviction continua mentre altri inseriscono e leggono
        inParallelo(50_000, () -> {
            String chiave = "k" + numero.incrementAndGet();
            limitatore.incrementa(chiave, ora);
            limitatore.conteggio(chiave, ora);
            return true;
        });
        assertEquals(1_000, limitatore.dimensione());
    }

    @Test
    void ilConteggioDiUnaChiaveNonPerdeIncrementiConPiuThread() throws Exception {
        LimitatoreAFinestra limitatore = new LimitatoreAFinestra(1_000, Duration.ofMinutes(15));
        Instant ora = Instant.parse("2026-11-20T10:00:00Z");
        inParallelo(20_000, () -> {
            limitatore.incrementa("stessa", ora);
            return true;
        });
        assertEquals(20_000, limitatore.conteggio("stessa", ora));
        assertTrue(limitatore.dimensione() <= 1_000);
    }
}
