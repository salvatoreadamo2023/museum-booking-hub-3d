package com.museumhub.booking.controller;

import com.jayway.jsonpath.JsonPath;
import com.museumhub.booking.support.ApiTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.LocalDate;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserControllerTest extends ApiTestBase {

    private static final String NUOVO = """
            {"nome":"Staff Museo","email":"Staff@Test.it","password":"password123","ruolo":"ADMIN"}""";

    private String modifica(String nome, String email, String ruolo, String stato, String password) {
        String pw = password == null ? "" : ",\"password\":\"" + password + "\"";
        return "{\"nome\":\"" + nome + "\",\"email\":\"" + email + "\",\"ruolo\":\"" + ruolo
                + "\",\"stato\":\"" + stato + "\"" + pw + "}";
    }

    @Test
    void ilVisitatoreNonPuoGestireGliUtenti() throws Exception {
        mockMvc.perform(get("/api/users").header(AUTH, bearer(registra("a@test.it"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminVedeLElencoSenzaPassword() throws Exception {
        registra("a@test.it");
        mockMvc.perform(get("/api/users").header(AUTH, bearer(tokenAdmin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenuto", hasSize(2)))
                .andExpect(jsonPath("$.totaleElementi").value(2))
                .andExpect(jsonPath("$.contenuto[0].email").value("admin@test.it"))
                .andExpect(jsonPath("$.contenuto[0].password").doesNotExist())
                .andExpect(jsonPath("$.contenuto[0].passwordHash").doesNotExist());
    }

    @Test
    void adminCreaUtenteAttivoDiDefaultConEmailInMinuscolo() throws Exception {
        mockMvc.perform(post("/api/users").header(AUTH, bearer(tokenAdmin())).contentType(JSON).content(NUOVO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("staff@test.it"))
                .andExpect(jsonPath("$.ruolo").value("ADMIN"))
                .andExpect(jsonPath("$.stato").value("ATTIVO"));
    }

    @Test
    void emailDuplicataDa409() throws Exception {
        mockMvc.perform(post("/api/users").header(AUTH, bearer(tokenAdmin())).contentType(JSON)
                        .content(NUOVO.replace("Staff@Test.it", "admin@test.it")))
                .andExpect(status().isConflict());
    }

    @Test
    void passwordCortaDa400() throws Exception {
        mockMvc.perform(post("/api/users").header(AUTH, bearer(tokenAdmin())).contentType(JSON)
                        .content(NUOVO.replace("password123", "123")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campi.password").exists());
    }

    @Test
    void sospendereUnUtenteBloccaSubitoLoroAccesso() throws Exception {
        MockHttpServletResponse registrazione = registraRisposta("a@test.it");
        String token = cookie(registrazione, "mbh_access");
        int id = JsonPath.read(registrazione.getContentAsString(), "$.id");
        mockMvc.perform(get("/api/tours").header(AUTH, bearer(token))).andExpect(status().isOk());

        mockMvc.perform(put("/api/users/" + id).header(AUTH, bearer(tokenAdmin())).contentType(JSON)
                        .content(modifica("Visitatore", "a@test.it", "VISITATORE", "SOSPESO", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stato").value("SOSPESO"));

        // il vecchio token non vale piu' e il login e' rifiutato
        mockMvc.perform(get("/api/tours").header(AUTH, bearer(token))).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/auth/login").contentType(JSON)
                        .content("{\"email\":\"a@test.it\",\"password\":\"password123\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void laNuovaPasswordPermetteIlLogin() throws Exception {
        int id = JsonPath.read(registraCorpo("a@test.it"), "$.id");
        mockMvc.perform(put("/api/users/" + id).header(AUTH, bearer(tokenAdmin())).contentType(JSON)
                        .content(modifica("Visitatore", "a@test.it", "VISITATORE", "ATTIVO", "nuovaPassword1")))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/auth/login").contentType(JSON)
                        .content("{\"email\":\"a@test.it\",\"password\":\"nuovaPassword1\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void lAdminNonPuoCambiareIlProprioRuoloOStato() throws Exception {
        int adminId = idAdmin();
        mockMvc.perform(put("/api/users/" + adminId).header(AUTH, bearer(tokenAdmin())).contentType(JSON)
                        .content(modifica("Admin Test", "admin@test.it", "VISITATORE", "ATTIVO", null)))
                .andExpect(status().isConflict());
    }

    @Test
    void lAdminNonPuoEliminareSeStesso() throws Exception {
        mockMvc.perform(delete("/api/users/" + idAdmin()).header(AUTH, bearer(tokenAdmin())))
                .andExpect(status().isConflict());
    }

    @Test
    void adminEliminaUnUtenteSenzaPrenotazioni() throws Exception {
        int id = JsonPath.read(registraCorpo("a@test.it"), "$.id");
        mockMvc.perform(delete("/api/users/" + id).header(AUTH, bearer(tokenAdmin())))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/users").header(AUTH, bearer(tokenAdmin())))
                .andExpect(jsonPath("$.contenuto", hasSize(1)));
    }

    @Test
    void unUtenteConPrenotazioniNonSiElimina() throws Exception {
        int tour = creaTour();
        int id = JsonPath.read(registraCorpo("a@test.it"), "$.id");
        inserisciPrenotazione("a@test.it", tour, LocalDate.now().plusDays(3), "09:30", 1);
        mockMvc.perform(delete("/api/users/" + id).header(AUTH, bearer(tokenAdmin())))
                .andExpect(status().isConflict());
    }

    @Test
    void utenteInesistenteDa404() throws Exception {
        mockMvc.perform(delete("/api/users/99999").header(AUTH, bearer(tokenAdmin())))
                .andExpect(status().isNotFound());
    }
}
