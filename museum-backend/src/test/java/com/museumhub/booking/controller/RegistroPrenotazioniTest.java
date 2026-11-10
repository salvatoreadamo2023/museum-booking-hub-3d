package com.museumhub.booking.controller;

import com.museumhub.booking.support.ApiTestBase;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Test del registro paginato delle prenotazioni (solo admin). */
class RegistroPrenotazioniTest extends ApiTestBase {

    /** Crea tre prenotazioni in tre giorni futuri diversi: oggi+1, oggi+2, oggi+3 (alle 09:30). */
    private void creaTrePrenotazioni() throws Exception {
        int tour = creaTour();
        long intero = tariffaId("Intero");
        String visitatore = registra("registro@test.it");
        for (int giorni = 1; giorni <= 3; giorni++) {
            prenota(visitatore, bodyPrenotazione(tour, LocalDate.now().plusDays(giorni), "09:30", posto(giorni, intero)))
                    .andExpect(status().isCreated());
        }
    }

    private String registro(String parametri) {
        return "/api/reservations/registro" + parametri;
    }

    @Test
    void ilRegistroEPaginatoEOrdinatoDallaVisitaPiuRecente() throws Exception {
        creaTrePrenotazioni();
        String admin = tokenAdmin();

        mockMvc.perform(get(registro("?pagina=0&dimensione=2")).header(AUTH, bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenuto", hasSize(2)))
                .andExpect(jsonPath("$.contenuto[0].data").value(LocalDate.now().plusDays(3).toString()))
                .andExpect(jsonPath("$.contenuto[1].data").value(LocalDate.now().plusDays(2).toString()))
                .andExpect(jsonPath("$.pagina").value(0))
                .andExpect(jsonPath("$.dimensione").value(2))
                .andExpect(jsonPath("$.totaleElementi").value(3))
                .andExpect(jsonPath("$.totalePagine").value(2));

        mockMvc.perform(get(registro("?pagina=1&dimensione=2")).header(AUTH, bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenuto", hasSize(1)))
                .andExpect(jsonPath("$.contenuto[0].data").value(LocalDate.now().plusDays(1).toString()))
                .andExpect(jsonPath("$.contenuto[0].utenteEmail").value("registro@test.it"))
                .andExpect(jsonPath("$.pagina").value(1));
    }

    @Test
    void senzaParametriUsaLaPrimaPaginaDaVentiRighe() throws Exception {
        creaTrePrenotazioni();

        mockMvc.perform(get(registro("")).header(AUTH, bearer(tokenAdmin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagina").value(0))
                .andExpect(jsonPath("$.dimensione").value(20))
                .andExpect(jsonPath("$.contenuto", hasSize(3)))
                .andExpect(jsonPath("$.totalePagine").value(1));
    }

    @Test
    void iParametriFuoriLimiteVengonoRiportatiNeiLimiti() throws Exception {
        creaTrePrenotazioni();
        String admin = tokenAdmin();

        mockMvc.perform(get(registro("?pagina=-5&dimensione=100000")).header(AUTH, bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagina").value(0))
                .andExpect(jsonPath("$.dimensione").value(100));
        mockMvc.perform(get(registro("?dimensione=0")).header(AUTH, bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dimensione").value(1))
                .andExpect(jsonPath("$.contenuto", hasSize(1)))
                .andExpect(jsonPath("$.totalePagine").value(3));
    }

    @Test
    void unaPaginaOltreLaFineEVuotaMaIndicaIlTotale() throws Exception {
        creaTrePrenotazioni();

        mockMvc.perform(get(registro("?pagina=9&dimensione=2")).header(AUTH, bearer(tokenAdmin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenuto", hasSize(0)))
                .andExpect(jsonPath("$.totaleElementi").value(3));
    }

    @Test
    void parametriNonNumericiDanno400() throws Exception {
        mockMvc.perform(get(registro("?pagina=abc")).header(AUTH, bearer(tokenAdmin())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void ilRegistroEriservatoAdAdmin() throws Exception {
        mockMvc.perform(get(registro("")).header(AUTH, bearer(registra("v@test.it"))))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(registro(""))).andExpect(status().isUnauthorized());
    }

    @Test
    void lElencoSempliceMostraSoloLePrenotazioniDiChiLoRichiede() throws Exception {
        creaTrePrenotazioni();

        mockMvc.perform(get("/api/reservations").header(AUTH, bearer(tokenAdmin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
        mockMvc.perform(get("/api/reservations").header(AUTH, bearer(registra("registro2@test.it"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }
}
