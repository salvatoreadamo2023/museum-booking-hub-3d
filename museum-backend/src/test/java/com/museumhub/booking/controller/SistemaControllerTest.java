package com.museumhub.booking.controller;

import com.museumhub.booking.monitor.RequestStats;
import com.museumhub.booking.support.ApiTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Test dell'endpoint di monitoraggio (solo admin). */
class SistemaControllerTest extends ApiTestBase {

    private static final String SISTEMA = "/api/admin/sistema";

    @Autowired
    private RequestStats stats;

    @Test
    void ladminVedeLeMetricheDelSistema() throws Exception {
        mockMvc.perform(get(SISTEMA).header(AUTH, bearer(tokenAdmin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rilevatoIl", notNullValue()))
                .andExpect(jsonPath("$.processore.core", greaterThan(0)))
                .andExpect(jsonPath("$.memoria.heapUsatoMb", greaterThan(0)))
                .andExpect(jsonPath("$.memoria.heapMassimoMb", greaterThan(0)))
                .andExpect(jsonPath("$.memoria.gcConteggio", greaterThanOrEqualTo(0)))
                .andExpect(jsonPath("$.concorrenza.threadAttivi", greaterThan(0)))
                .andExpect(jsonPath("$.richieste.totali", greaterThanOrEqualTo(0)))
                .andExpect(jsonPath("$.richieste.tempoMedioMs", greaterThanOrEqualTo(0.0)));
    }

    @Test
    void ilVisitatoreNonPuoVedereLeMetriche() throws Exception {
        mockMvc.perform(get(SISTEMA).header(AUTH, bearer(registra("v@test.it"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void senzaLoginDa401() throws Exception {
        mockMvc.perform(get(SISTEMA)).andExpect(status().isUnauthorized());
    }

    @Test
    void leRichiesteVengonoContateMaLaPaginaDiMonitoraggioNo() throws Exception {
        String admin = tokenAdmin();
        long totaliPrima = stats.getTotali();

        mockMvc.perform(get("/api/tours").header(AUTH, bearer(admin))).andExpect(status().isOk());
        mockMvc.perform(get("/api/tours")).andExpect(status().isUnauthorized());
        mockMvc.perform(get(SISTEMA).header(AUTH, bearer(admin))).andExpect(status().isOk());
        mockMvc.perform(get(SISTEMA).header(AUTH, bearer(admin))).andExpect(status().isOk());

        assertEquals(totaliPrima + 2, stats.getTotali());
    }
}
