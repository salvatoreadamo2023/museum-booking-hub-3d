package com.museumhub.booking.controller;

import com.museumhub.booking.support.ApiTestBase;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Un itinerario può avere al massimo 20 posti per fascia oraria. */
class CapienzaMassimaTest extends ApiTestBase {

    private String tour(int capienza) {
        return "{\"titolo\":\"Sala piccola\",\"descrizione\":\"Prova\",\"durataMin\":45,\"prezzo\":10.00,"
                + "\"capienza\":" + capienza + ",\"orari\":[\"10:00\"]}";
    }

    @Test
    void ventiPostiSonoAccettati() throws Exception {
        mockMvc.perform(post("/api/tours").header(AUTH, bearer(tokenAdmin())).contentType(JSON).content(tour(20)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.capienza").value(20));
    }

    @Test
    void ventunoPostiDa400() throws Exception {
        mockMvc.perform(post("/api/tours").header(AUTH, bearer(tokenAdmin())).contentType(JSON).content(tour(21)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campi.capienza").exists());
    }
}
