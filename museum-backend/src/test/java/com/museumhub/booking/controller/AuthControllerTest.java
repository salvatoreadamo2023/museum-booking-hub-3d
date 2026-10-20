package com.museumhub.booking.controller;

import com.museumhub.booking.entity.Ruolo;
import com.museumhub.booking.entity.StatoAccount;
import com.museumhub.booking.entity.User;
import com.museumhub.booking.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Test funzionali dell'autenticazione: ogni test e' annullato a fine esecuzione (@Transactional). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private String json(String email, String password) {
        return "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
    }

    private String registrazione(String nome, String email, String password) {
        return "{\"nome\":\"" + nome + "\",\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
    }

    @Test
    void loginAdminDalleCredenzialiIniziali() throws Exception {
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json("admin@test.it", "PasswordAdmin1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.ruolo").value("ADMIN"));
    }

    @Test
    void loginConPasswordSbagliataDa401() throws Exception {
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json("admin@test.it", "sbagliata")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.messaggio").value("Credenziali non valide"));
    }

    @Test
    void loginAccountSospesoDa403() throws Exception {
        userRepository.save(User.builder().nome("Sospeso").email("sospeso@test.it")
                .passwordHash(passwordEncoder.encode("Password123"))
                .ruolo(Ruolo.VISITATORE).stato(StatoAccount.SOSPESO).build());

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json("sospeso@test.it", "Password123")))
                .andExpect(status().isForbidden());
    }

    @Test
    void registrazioneCreaVisitatoreEFaEntrare() throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(registrazione("Mario Rossi", "Mario@Test.it", "password123")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.ruolo").value("VISITATORE"))
                .andExpect(jsonPath("$.email").value("mario@test.it"));
    }

    @Test
    void registrazioneConEmailGiaPresenteDa409() throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(registrazione("Altro Admin", "admin@test.it", "password123")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.messaggio").value("Email già registrata"));
    }

    @Test
    void registrazioneConPasswordCortaDa400() throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(registrazione("Mario", "mario2@test.it", "123")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campi.password").exists());
    }

    @Test
    void registrazioneSenzaNomeDa400() throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(registrazione("", "mario3@test.it", "password123")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campi.nome").exists());
    }

    @Test
    void endpointProtettoSenzaTokenDa401() throws Exception {
        mockMvc.perform(get("/api/tours"))
                .andExpect(status().isUnauthorized());
    }
}
