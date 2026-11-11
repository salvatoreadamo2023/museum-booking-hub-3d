package com.museumhub.booking.controller;

import com.jayway.jsonpath.JsonPath;
import com.museumhub.booking.entity.Sessione;
import com.museumhub.booking.repository.SessioneRepository;
import com.museumhub.booking.security.JwtService;
import com.museumhub.booking.support.ApiTestBase;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test delle misure di sicurezza: cookie HttpOnly/SameSite, sessioni revocabili, refresh token, limite ai
 * tentativi di login e intestazioni di sicurezza. Ogni test usa email diverse perche' il contatore dei tentativi
 * e' in memoria e non viene annullato dal rollback del database.
 */
class SicurezzaTest extends ApiTestBase {

    private static final String ACCESSO = "mbh_access";
    private static final String REFRESH = "mbh_refresh";

    @Autowired
    private SessioneRepository sessioneRepository;
    @Autowired
    private JwtService jwtService;

    private String loginJson(String email, String password) {
        return "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
    }

    private int login(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/login").contentType(JSON).content(loginJson(email, password)))
                .andReturn().getResponse().getStatus();
    }

    private String headerCookie(MockHttpServletResponse risposta, String nome) {
        for (String header : risposta.getHeaders("Set-Cookie")) {
            if (header.startsWith(nome + "=")) {
                return header;
            }
        }
        return null;
    }

    @Test
    void loginImpostaCookieHttpOnlyESameSiteENonRestituisceIlToken() throws Exception {
        MockHttpServletResponse risposta = registraRisposta("cookie@test.it");
        String accesso = headerCookie(risposta, ACCESSO);
        String refresh = headerCookie(risposta, REFRESH);

        assertNotNull(accesso);
        assertNotNull(refresh);
        for (String header : List.of(accesso, refresh)) {
            assertTrue(header.contains("HttpOnly"), header);
            assertTrue(header.contains("SameSite=Strict"), header);
        }
        assertTrue(accesso.contains("Path=/api"), accesso);
        assertTrue(refresh.contains("Path=/api/auth"), refresh);
        // il token non e' nel corpo della risposta, solo nei cookie
        assertTrue(JsonPath.<java.util.Map<String, Object>>read(risposta.getContentAsString(), "$").get("token") == null);
    }

    @Test
    void ilCookieDiAccessoAutenticaLeRichieste() throws Exception {
        String token = registra("cookie2@test.it");
        mockMvc.perform(get("/api/tours").cookie(new Cookie(ACCESSO, token))).andExpect(status().isOk());
        mockMvc.perform(get("/api/tours")).andExpect(status().isUnauthorized());
    }

    @Test
    void meRestituisceLUtenteDellaSessione() throws Exception {
        String token = registra("me@test.it");
        mockMvc.perform(get("/api/auth/me").cookie(new Cookie(ACCESSO, token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("me@test.it"))
                .andExpect(jsonPath("$.ruolo").value("VISITATORE"));
        mockMvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void ilRefreshTokenNonVaBeneComeTokenDiAccesso() throws Exception {
        String refresh = cookie(registraRisposta("tipo@test.it"), REFRESH);
        mockMvc.perform(get("/api/tours").cookie(new Cookie(ACCESSO, refresh))).andExpect(status().isUnauthorized());
    }

    @Test
    void ilRefreshEmetteUnNuovoTokenDiAccesso() throws Exception {
        String refresh = cookie(registraRisposta("refresh@test.it"), REFRESH);

        MockHttpServletResponse risposta = mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new Cookie(REFRESH, refresh)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("refresh@test.it"))
                .andReturn().getResponse();

        String nuovo = cookie(risposta, ACCESSO);
        assertNotNull(nuovo);
        mockMvc.perform(get("/api/tours").cookie(new Cookie(ACCESSO, nuovo))).andExpect(status().isOk());
    }

    @Test
    void ilRefreshSenzaCookieODiTokenSbagliatoDa401() throws Exception {
        mockMvc.perform(post("/api/auth/refresh")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/auth/refresh").cookie(new Cookie(REFRESH, "non-un-token")))
                .andExpect(status().isUnauthorized());
        String accesso = registra("refresh2@test.it");
        mockMvc.perform(post("/api/auth/refresh").cookie(new Cookie(REFRESH, accesso)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void ilLogoutRevocaLaSessioneECancellaICookie() throws Exception {
        MockHttpServletResponse reg = registraRisposta("logout@test.it");
        String accesso = cookie(reg, ACCESSO);
        String refresh = cookie(reg, REFRESH);
        mockMvc.perform(get("/api/tours").cookie(new Cookie(ACCESSO, accesso))).andExpect(status().isOk());

        MockHttpServletResponse risposta = mockMvc.perform(post("/api/auth/logout")
                        .cookie(new Cookie(ACCESSO, accesso), new Cookie(REFRESH, refresh)))
                .andExpect(status().isNoContent())
                .andReturn().getResponse();
        assertTrue(headerCookie(risposta, ACCESSO).contains("Max-Age=0"));
        assertTrue(headerCookie(risposta, REFRESH).contains("Max-Age=0"));

        // dopo il logout il token di accesso e il refresh token non valgono piu'
        mockMvc.perform(get("/api/tours").cookie(new Cookie(ACCESSO, accesso))).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/auth/refresh").cookie(new Cookie(REFRESH, refresh)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void ilLogoutSenzaSessioneNonDaErrore() throws Exception {
        mockMvc.perform(post("/api/auth/logout")).andExpect(status().isNoContent());
    }

    @Test
    void unaSessioneScadutaNonAutentica() throws Exception {
        String token = registra("scaduta@test.it");
        Sessione sessione = sessioneRepository.findById(jwtService.extractSid(token)).orElseThrow();
        sessione.setScadeIl(Instant.now().minusSeconds(60));
        sessioneRepository.saveAndFlush(sessione);

        mockMvc.perform(get("/api/tours").cookie(new Cookie(ACCESSO, token))).andExpect(status().isUnauthorized());
    }

    @Test
    void cambiareLaPasswordDiUnUtenteChiudeLeSueSessioni() throws Exception {
        String corpo = registraCorpo("pw@test.it");
        int id = JsonPath.read(corpo, "$.id");
        String token = cookie(registraRisposta("pw2@test.it"), ACCESSO);
        mockMvc.perform(get("/api/tours").cookie(new Cookie(ACCESSO, token))).andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/login").contentType(JSON).content(loginJson("pw@test.it", "password123")))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/users/" + id).header(AUTH, bearer(tokenAdmin())).contentType(JSON)
                        .content("{\"nome\":\"Visitatore\",\"email\":\"pw@test.it\",\"ruolo\":\"VISITATORE\","
                                + "\"stato\":\"ATTIVO\",\"password\":\"nuovaPassword1\"}"))
                .andExpect(status().isOk());

        assertEquals(0, sessioneRepository.findAll().stream().filter(s -> s.getUserId() == id).count());
        // l'altro utente non e' toccato
        mockMvc.perform(get("/api/tours").cookie(new Cookie(ACCESSO, token))).andExpect(status().isOk());
    }

    @Test
    void eliminareUnUtenteEliminaAncheLeSueSessioni() throws Exception {
        int id = JsonPath.read(registraCorpo("elimina@test.it"), "$.id");
        assertTrue(sessioneRepository.findAll().stream().anyMatch(s -> s.getUserId() == id));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/users/" + id)
                        .header(AUTH, bearer(tokenAdmin())))
                .andExpect(status().isNoContent());

        assertEquals(0, sessioneRepository.findAll().stream().filter(s -> s.getUserId() == id).count());
    }

    @Test
    void dopoTroppiTentativiErratiIlLoginEBloccatoAncheConLaPasswordGiusta() throws Exception {
        registraRisposta("forza@test.it");
        for (int i = 0; i < 5; i++) {
            assertEquals(401, login("forza@test.it", "sbagliata" + i));
        }
        assertEquals(429, login("forza@test.it", "password123"));
        // un altro account non e' coinvolto
        assertEquals(200, login("admin@test.it", "PasswordAdmin1"));
    }

    @Test
    void ilBloccoValeAncheSuEmailInesistenti() throws Exception {
        for (int i = 0; i < 5; i++) {
            assertEquals(401, login("inesistente@test.it", "qualunque1"));
        }
        assertEquals(429, login("inesistente@test.it", "qualunque1"));
    }

    @Test
    void unAccessoRiuscitoAzzeraIlContatoreDegliErrori() throws Exception {
        registraRisposta("azzera@test.it");
        for (int i = 0; i < 4; i++) {
            assertEquals(401, login("azzera@test.it", "sbagliata"));
        }
        assertEquals(200, login("azzera@test.it", "password123"));
        for (int i = 0; i < 4; i++) {
            assertEquals(401, login("azzera@test.it", "sbagliata"));
        }
        assertEquals(200, login("azzera@test.it", "password123"));
    }

    @Test
    void leRisposteHannoLeIntestazioniDiSicurezza() throws Exception {
        // anche una risposta 401 le contiene
        MockHttpServletResponse risposta = mockMvc.perform(get("/api/tours"))
                .andExpect(status().isUnauthorized()).andReturn().getResponse();
        assertEquals("nosniff", risposta.getHeader("X-Content-Type-Options"));
        assertEquals("DENY", risposta.getHeader("X-Frame-Options"));
        assertEquals("no-referrer", risposta.getHeader("Referrer-Policy"));
        assertTrue(risposta.getHeader("Content-Security-Policy").contains("frame-ancestors 'none'"));
        assertNotNull(risposta.getHeader("Permissions-Policy"));
    }
}
