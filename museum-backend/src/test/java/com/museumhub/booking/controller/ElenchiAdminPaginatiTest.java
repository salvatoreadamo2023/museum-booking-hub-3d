package com.museumhub.booking.controller;

import com.museumhub.booking.support.ApiTestBase;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Test della paginazione degli elenchi utenti e feedback (solo admin). */
class ElenchiAdminPaginatiTest extends ApiTestBase {

    private String feedback(int tourId, int voto) {
        return "{\"tourId\":" + tourId + ",\"voto\":" + voto + ",\"commento\":\"Commento " + voto + "\"}";
    }

    /** Tre feedback (voto 1, 2, 3, inseriti in quest'ordine) di un visitatore con tre visite gia' concluse. */
    private void creaTreFeedback() throws Exception {
        int tour = creaTour();
        String token = registra("fb@test.it");
        for (int voto = 1; voto <= 3; voto++) {
            inserisciPrenotazione("fb@test.it", tour, LocalDate.now().minusDays(voto), "09:30", voto);
            mockMvc.perform(post("/api/feedback").header(AUTH, bearer(token)).contentType(JSON).content(feedback(tour, voto)))
                    .andExpect(status().isCreated());
        }
    }

    @Test
    void gliUtentiSonoPaginatiPerId() throws Exception {
        registra("u1@test.it");
        registra("u2@test.it");
        registra("u3@test.it");
        String admin = tokenAdmin();

        mockMvc.perform(get("/api/users?pagina=0&dimensione=3").header(AUTH, bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenuto", hasSize(3)))
                .andExpect(jsonPath("$.contenuto[0].email").value("admin@test.it"))
                .andExpect(jsonPath("$.pagina").value(0))
                .andExpect(jsonPath("$.dimensione").value(3))
                .andExpect(jsonPath("$.totaleElementi").value(4))
                .andExpect(jsonPath("$.totalePagine").value(2));
        mockMvc.perform(get("/api/users?pagina=1&dimensione=3").header(AUTH, bearer(admin)))
                .andExpect(jsonPath("$.contenuto", hasSize(1)))
                .andExpect(jsonPath("$.contenuto[0].email").value("u3@test.it"));
    }

    @Test
    void iParametriDegliUtentiFuoriLimiteVengonoRiportatiNeiLimiti() throws Exception {
        String admin = tokenAdmin();

        mockMvc.perform(get("/api/users?pagina=-3&dimensione=100000").header(AUTH, bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagina").value(0))
                .andExpect(jsonPath("$.dimensione").value(100));
        mockMvc.perform(get("/api/users?dimensione=0").header(AUTH, bearer(admin)))
                .andExpect(jsonPath("$.dimensione").value(1))
                .andExpect(jsonPath("$.contenuto", hasSize(1)));
        mockMvc.perform(get("/api/users?pagina=9").header(AUTH, bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenuto", hasSize(0)));
        mockMvc.perform(get("/api/users?pagina=abc").header(AUTH, bearer(admin)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void iFeedbackSonoPaginatiDalPiuRecente() throws Exception {
        creaTreFeedback();
        String admin = tokenAdmin();

        mockMvc.perform(get("/api/feedback?pagina=0&dimensione=2").header(AUTH, bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenuto", hasSize(2)))
                .andExpect(jsonPath("$.contenuto[0].voto").value(3))
                .andExpect(jsonPath("$.contenuto[1].voto").value(2))
                .andExpect(jsonPath("$.totaleElementi").value(3))
                .andExpect(jsonPath("$.totalePagine").value(2));
        mockMvc.perform(get("/api/feedback?pagina=1&dimensione=2").header(AUTH, bearer(admin)))
                .andExpect(jsonPath("$.contenuto", hasSize(1)))
                .andExpect(jsonPath("$.contenuto[0].voto").value(1))
                .andExpect(jsonPath("$.contenuto[0].utente").value("Visitatore fb@test.it"));
    }

    @Test
    void ilFeedbackFuoriLimiteEVuotoOLimitato() throws Exception {
        creaTreFeedback();
        String admin = tokenAdmin();

        mockMvc.perform(get("/api/feedback?pagina=9&dimensione=2").header(AUTH, bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenuto", hasSize(0)))
                .andExpect(jsonPath("$.totaleElementi").value(3));
        mockMvc.perform(get("/api/feedback?dimensione=100000").header(AUTH, bearer(admin)))
                .andExpect(jsonPath("$.dimensione").value(100));
    }

    @Test
    void gliItinerariDiGestioneSonoPaginati() throws Exception {
        creaTour();
        creaTour();
        creaTour();
        String admin = tokenAdmin();

        mockMvc.perform(get("/api/tours/gestione?pagina=0&dimensione=2").header(AUTH, bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenuto", hasSize(2)))
                .andExpect(jsonPath("$.contenuto[0].orari").exists())
                .andExpect(jsonPath("$.totaleElementi").value(3))
                .andExpect(jsonPath("$.totalePagine").value(2));
        mockMvc.perform(get("/api/tours/gestione?pagina=1&dimensione=2").header(AUTH, bearer(admin)))
                .andExpect(jsonPath("$.contenuto", hasSize(1)));
        mockMvc.perform(get("/api/tours/gestione?dimensione=100000").header(AUTH, bearer(admin)))
                .andExpect(jsonPath("$.dimensione").value(100));
    }

    @Test
    void gliItinerariDiGestioneSonoSoloPerLAdmin() throws Exception {
        mockMvc.perform(get("/api/tours/gestione").header(AUTH, bearer(registra("v@test.it"))))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/tours/gestione")).andExpect(status().isUnauthorized());
    }

    @Test
    void senzaFeedbackLaPaginaEVuota() throws Exception {
        mockMvc.perform(get("/api/feedback").header(AUTH, bearer(tokenAdmin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenuto", hasSize(0)))
                .andExpect(jsonPath("$.totaleElementi").value(0))
                .andExpect(jsonPath("$.totalePagine").value(0));
    }
}
