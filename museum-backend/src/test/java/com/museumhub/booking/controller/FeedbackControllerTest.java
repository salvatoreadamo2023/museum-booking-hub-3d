package com.museumhub.booking.controller;

import com.museumhub.booking.support.ApiTestBase;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FeedbackControllerTest extends ApiTestBase {

    private String feedback(int tourId, int voto) {
        return "{\"tourId\":" + tourId + ",\"voto\":" + voto + ",\"commento\":\"Visita bellissima\"}";
    }

    @Test
    void senzaUnaVisitaNonSiPuoLasciareUnFeedback() throws Exception {
        int tour = creaTour();
        mockMvc.perform(post("/api/feedback").header(AUTH, bearer(registra("a@test.it")))
                        .contentType(JSON).content(feedback(tour, 5)))
                .andExpect(status().isForbidden());
    }

    @Test
    void conUnaVisitaFutura403() throws Exception {
        int tour = creaTour();
        String token = registra("a@test.it");
        inserisciPrenotazione("a@test.it", tour, LocalDate.now().plusDays(3), "09:30", 1);
        mockMvc.perform(post("/api/feedback").header(AUTH, bearer(token)).contentType(JSON).content(feedback(tour, 5)))
                .andExpect(status().isForbidden());
    }

    @Test
    void dopoLaVisitaIlFeedbackEAccettatoUnaVoltaPerVisita() throws Exception {
        int tour = creaTour();
        String token = registra("a@test.it");
        inserisciPrenotazione("a@test.it", tour, LocalDate.now().minusDays(2), "09:30", 1);

        mockMvc.perform(post("/api/feedback").header(AUTH, bearer(token)).contentType(JSON).content(feedback(tour, 5)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.voto").value(5))
                .andExpect(jsonPath("$.tour").value("Tour Test"));
        mockMvc.perform(post("/api/feedback").header(AUTH, bearer(token)).contentType(JSON).content(feedback(tour, 4)))
                .andExpect(status().isConflict());
    }

    @Test
    void unaSecondaVisitaPermetteUnSecondoFeedback() throws Exception {
        int tour = creaTour();
        String token = registra("a@test.it");
        inserisciPrenotazione("a@test.it", tour, LocalDate.now().minusDays(5), "09:30", 1);
        inserisciPrenotazione("a@test.it", tour, LocalDate.now().minusDays(2), "11:00", 1);

        mockMvc.perform(post("/api/feedback").header(AUTH, bearer(token)).contentType(JSON).content(feedback(tour, 5)))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/feedback").header(AUTH, bearer(token)).contentType(JSON).content(feedback(tour, 4)))
                .andExpect(status().isCreated());
    }

    @Test
    void votoFuoriDa1a5Da400() throws Exception {
        int tour = creaTour();
        mockMvc.perform(post("/api/feedback").header(AUTH, bearer(registra("a@test.it")))
                        .contentType(JSON).content(feedback(tour, 6)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campi.voto").exists());
    }

    @Test
    void adminLeggeIFeedbackEIlVisitatoreNo() throws Exception {
        int tour = creaTour();
        String token = registra("a@test.it");
        inserisciPrenotazione("a@test.it", tour, LocalDate.now().minusDays(2), "09:30", 1);
        mockMvc.perform(post("/api/feedback").header(AUTH, bearer(token)).contentType(JSON).content(feedback(tour, 5)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/feedback").header(AUTH, bearer(tokenAdmin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].commento").value("Visita bellissima"));
        mockMvc.perform(get("/api/feedback").header(AUTH, bearer(token)))
                .andExpect(status().isForbidden());
    }

    @Test
    void lAdminNonLasciaFeedback() throws Exception {
        int tour = creaTour();
        mockMvc.perform(post("/api/feedback").header(AUTH, bearer(tokenAdmin()))
                        .contentType(JSON).content(feedback(tour, 5)))
                .andExpect(status().isForbidden());
    }
}
