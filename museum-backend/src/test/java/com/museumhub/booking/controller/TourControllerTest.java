package com.museumhub.booking.controller;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Test funzionali degli itinerari. Ogni test e' annullato a fine esecuzione (@Transactional). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TourControllerTest {

    private static final String TOUR = """
            {"titolo":"Tour Test","descrizione":"Percorso di prova","durataMin":60,
             "prezzo":12.00,"capienza":10,"orari":["11:00","09:30"]}""";

    @Autowired
    private MockMvc mockMvc;

    /** Il token di accesso arriva nel corpo della risposta di login e registrazione. */
    private String token(MockHttpServletResponse risposta) throws Exception {
        return JsonPath.read(risposta.getContentAsString(), "$.token");
    }

    private String tokenAdmin() throws Exception {
        return token(mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@test.it\",\"password\":\"PasswordAdmin1\"}"))
                .andReturn().getResponse());
    }

    private String tokenVisitatore() throws Exception {
        return token(mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Visitatore\",\"email\":\"visitatore@test.it\",\"password\":\"password123\"}"))
                .andReturn().getResponse());
    }

    /** Crea un itinerario come admin e ne restituisce l'id. */
    private int creaTour() throws Exception {
        String body = mockMvc.perform(post("/api/tours").header("Authorization", "Bearer " + tokenAdmin())
                        .contentType(MediaType.APPLICATION_JSON).content(TOUR))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    @Test
    void senzaTokenDa401() throws Exception {
        mockMvc.perform(get("/api/tours")).andExpect(status().isUnauthorized());
    }

    @Test
    void adminCreaItinerarioConOrariOrdinatiEPrezziPerTariffa() throws Exception {
        mockMvc.perform(post("/api/tours").header("Authorization", "Bearer " + tokenAdmin())
                        .contentType(MediaType.APPLICATION_JSON).content(TOUR))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.titolo").value("Tour Test"))
                .andExpect(jsonPath("$.orari.length()").value(2))
                .andExpect(jsonPath("$.orari[0].orario").value("09:30"))
                .andExpect(jsonPath("$.orari[0].liberi").value(10))
                .andExpect(jsonPath("$.tariffe[0].nome").value("Intero"))
                .andExpect(jsonPath("$.tariffe[0].prezzo").value(12.0))
                .andExpect(jsonPath("$.tariffe[1].nome").value("Under 16"))
                .andExpect(jsonPath("$.tariffe[1].prezzo").value(6.0));
    }

    @Test
    void visitatoreNonPuoCreareItinerari() throws Exception {
        mockMvc.perform(post("/api/tours").header("Authorization", "Bearer " + tokenVisitatore())
                        .contentType(MediaType.APPLICATION_JSON).content(TOUR))
                .andExpect(status().isForbidden());
    }

    @Test
    void visitatoreVedeGliItinerariConPostiLiberi() throws Exception {
        creaTour();
        mockMvc.perform(get("/api/tours").param("data", "2030-01-10")
                        .header("Authorization", "Bearer " + tokenVisitatore()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].orari[0].liberi").value(10));
    }

    @Test
    void datiNonValidiDanno400() throws Exception {
        String errato = """
                {"titolo":"","durataMin":60,"prezzo":12.00,"capienza":0,"orari":["25:00"]}""";
        mockMvc.perform(post("/api/tours").header("Authorization", "Bearer " + tokenAdmin())
                        .contentType(MediaType.APPLICATION_JSON).content(errato))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campi.titolo").exists())
                .andExpect(jsonPath("$.campi.capienza").exists());
    }

    @Test
    void adminModificaItinerarioEFasce() throws Exception {
        int id = creaTour();
        String nuovo = """
                {"titolo":"Nuovo titolo","descrizione":"x","durataMin":45,
                 "prezzo":15.00,"capienza":8,"orari":["10:00"]}""";
        mockMvc.perform(put("/api/tours/" + id).header("Authorization", "Bearer " + tokenAdmin())
                        .contentType(MediaType.APPLICATION_JSON).content(nuovo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titolo").value("Nuovo titolo"))
                .andExpect(jsonPath("$.capienza").value(8))
                .andExpect(jsonPath("$.orari.length()").value(1))
                .andExpect(jsonPath("$.orari[0].orario").value("10:00"));
    }

    @Test
    void adminEliminaItinerarioSenzaPrenotazioni() throws Exception {
        int id = creaTour();
        mockMvc.perform(delete("/api/tours/" + id).header("Authorization", "Bearer " + tokenAdmin()))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/tours").header("Authorization", "Bearer " + tokenAdmin()))
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void visitatoreNonPuoEliminare() throws Exception {
        int id = creaTour();
        mockMvc.perform(delete("/api/tours/" + id).header("Authorization", "Bearer " + tokenVisitatore()))
                .andExpect(status().isForbidden());
    }

    @Test
    void itinerarioInesistenteDa404() throws Exception {
        mockMvc.perform(delete("/api/tours/99999").header("Authorization", "Bearer " + tokenAdmin()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.messaggio").value("Itinerario non trovato"));
    }

    @Test
    void postiDiUnaFasciaSonoTuttiLiberi() throws Exception {
        int id = creaTour();
        mockMvc.perform(get("/api/tours/" + id + "/seats").param("data", "2030-01-10").param("orario", "09:30")
                        .header("Authorization", "Bearer " + tokenVisitatore()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.occupati").isEmpty())
                .andExpect(jsonPath("$.capienza").value(10))
                .andExpect(jsonPath("$.liberi").value(10));
    }

    @Test
    void postiDiUnaFasciaInesistenteDa404() throws Exception {
        int id = creaTour();
        mockMvc.perform(get("/api/tours/" + id + "/seats").param("data", "2030-01-10").param("orario", "15:00")
                        .header("Authorization", "Bearer " + tokenVisitatore()))
                .andExpect(status().isNotFound());
    }

    @Test
    void postiSenzaDataDa400() throws Exception {
        int id = creaTour();
        mockMvc.perform(get("/api/tours/" + id + "/seats").param("orario", "09:30")
                        .header("Authorization", "Bearer " + tokenVisitatore()))
                .andExpect(status().isBadRequest());
    }
}
