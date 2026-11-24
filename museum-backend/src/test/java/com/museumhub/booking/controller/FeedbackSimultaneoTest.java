package com.museumhub.booking.controller;

import com.museumhub.booking.repository.FeedbackRepository;
import com.museumhub.booking.support.ApiTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Lo stesso visitatore invia due feedback nello stesso istante per l'unica visita fatta (per esempio con un
 * doppio clic). Come nel test delle prenotazioni simultanee, ogni richiesta ha la propria transazione.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class FeedbackSimultaneoTest extends ApiTestBase {

    @Autowired
    private FeedbackRepository feedbackRepository;

    @Test
    void dueFeedbackSimultaneiPerUnaVisitaNePassaUnoSolo() throws Exception {
        int tour = creaTour();
        String token = registra("doppio@test.it");
        inserisciPrenotazione("doppio@test.it", tour, LocalDate.now().minusDays(2), "09:30", 1);
        String body = "{\"tourId\":" + tour + ",\"voto\":5,\"commento\":\"Visita bellissima\"}";

        CountDownLatch via = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        List<Future<Integer>> risposte = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            Callable<Integer> richiesta = () -> {
                via.await();
                return mockMvc.perform(post("/api/feedback").header(AUTH, bearer(token))
                                .contentType(JSON).content(body))
                        .andReturn().getResponse().getStatus();
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
        assertEquals(1, feedbackRepository.count());
    }
}
