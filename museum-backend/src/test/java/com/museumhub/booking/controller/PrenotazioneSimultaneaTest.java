package com.museumhub.booking.controller;

import com.museumhub.booking.support.ApiTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Due visitatori che prenotano lo stesso posto nello stesso istante. Le richieste partono da due thread e
 * ognuna ha la propria transazione, come in produzione: per questo la classe non usa la transazione di test
 * (che verrebbe annullata alla fine) e chiede un contesto nuovo dopo l'esecuzione, per non lasciare dati agli
 * altri test.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PrenotazioneSimultaneaTest extends ApiTestBase {

    @Test
    void dueRichiesteSimultaneeSulloStessoPostoNePassaUnaSola() throws Exception {
        int tour = creaTour();
        String body = bodyPrenotazione(tour, LocalDate.now().plusDays(7), "09:30", posto(5, tariffaId("Intero")));
        List<String> visitatori = List.of(registra("primo@test.it"), registra("secondo@test.it"));

        CountDownLatch via = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(visitatori.size());
        List<Future<Integer>> risposte = new ArrayList<>();
        for (String token : visitatori) {
            Callable<Integer> richiesta = () -> {
                via.await();
                return prenota(token, body).andReturn().getResponse().getStatus();
            };
            risposte.add(pool.submit(richiesta));
        }
        via.countDown();

        List<Integer> stati = new ArrayList<>();
        for (Future<Integer> r : risposte) {
            stati.add(r.get(30, TimeUnit.SECONDS));
        }
        pool.shutdown();

        assertTrue(stati.contains(201) && stati.contains(409), "stati ricevuti: " + stati);
        assertEquals(1, reservationRepository.count());
    }
}
