package com.museumhub.booking.controller;

import com.museumhub.booking.support.ApiTestBase;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StatsControllerTest extends ApiTestBase {

    @Test
    void soloLAdminVedeLeStatistiche() throws Exception {
        mockMvc.perform(get("/api/stats").header(AUTH, bearer(registra("a@test.it"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void periodoNonValidoDa400() throws Exception {
        mockMvc.perform(get("/api/stats").param("periodo", "anno").header(AUTH, bearer(tokenAdmin())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void senzaPrenotazioniGliIndicatoriSonoAZero() throws Exception {
        creaTour();
        mockMvc.perform(get("/api/stats").param("periodo", "7giorni").header(AUTH, bearer(tokenAdmin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.incasso").value(0.0))
                .andExpect(jsonPath("$.biglietti").value(0))
                .andExpect(jsonPath("$.oraDiPunta").value(nullValue()))
                .andExpect(jsonPath("$.incassoVariazione").value(nullValue()))
                .andExpect(jsonPath("$.andamento", hasSize(7)));
    }

    @Test
    void gliIndicatoriSiCalcolanoDaiPostiPrenotati() throws Exception {
        int tour = creaTour();
        registra("a@test.it");
        inserisciPrenotazione("a@test.it", tour, LocalDate.now().minusDays(1), "09:30", 1, 2);
        inserisciPrenotazione("a@test.it", tour, LocalDate.now().minusDays(1), "11:00", 3);

        mockMvc.perform(get("/api/stats").param("periodo", "7giorni").header(AUTH, bearer(tokenAdmin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.incasso").value(36.0))
                .andExpect(jsonPath("$.biglietti").value(3))
                .andExpect(jsonPath("$.oraDiPunta").value("09:30"))
                .andExpect(jsonPath("$.postiRiservati").value(3))
                .andExpect(jsonPath("$.postiTotali").value(140))
                .andExpect(jsonPath("$.andamento", hasSize(7)))
                .andExpect(jsonPath("$.andamento[5].biglietti").value(3))
                .andExpect(jsonPath("$.itinerari[0].biglietti").value(3))
                .andExpect(jsonPath("$.itinerari[0].percentuale").value(100.0));
    }

    @Test
    void ilPeriodoOggiNonContaIeri() throws Exception {
        int tour = creaTour();
        registra("a@test.it");
        inserisciPrenotazione("a@test.it", tour, LocalDate.now().minusDays(1), "09:30", 1);

        mockMvc.perform(get("/api/stats").param("periodo", "oggi").header(AUTH, bearer(tokenAdmin())))
                .andExpect(jsonPath("$.biglietti").value(0))
                .andExpect(jsonPath("$.andamento", hasSize(1)));
    }

    @Test
    void laVariazioneConfrontaIlPeriodoPrecedente() throws Exception {
        int tour = creaTour();
        registra("a@test.it");
        // 1 biglietto negli ultimi 7 giorni, 2 nei 7 giorni prima: variazione -50%
        inserisciPrenotazione("a@test.it", tour, LocalDate.now().minusDays(1), "09:30", 1);
        inserisciPrenotazione("a@test.it", tour, LocalDate.now().minusDays(9), "09:30", 1, 2);

        mockMvc.perform(get("/api/stats").param("periodo", "7giorni").header(AUTH, bearer(tokenAdmin())))
                .andExpect(jsonPath("$.bigliettiVariazione").value(-50.0))
                .andExpect(jsonPath("$.incassoVariazione").value(-50.0));
    }
}
