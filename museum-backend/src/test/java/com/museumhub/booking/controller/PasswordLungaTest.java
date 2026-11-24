package com.museumhub.booking.controller;

import com.museumhub.booking.support.ApiTestBase;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** BCrypt usa solo i primi 72 byte della password: il limite si controlla in byte UTF-8, non in caratteri. */
class PasswordLungaTest extends ApiTestBase {

    private String registrazione(String email, String password) {
        return "{\"nome\":\"Mario Rossi\",\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
    }

    @Test
    void passwordDi40LettereAccentateSono80ByteEDa400() throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(JSON)
                        .content(registrazione("lunga@test.it", "è".repeat(40))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campi.password").exists());
    }

    @Test
    void passwordDi36LettereAccentateSono72ByteEFunziona() throws Exception {
        String password = "è".repeat(36);
        mockMvc.perform(post("/api/auth/register").contentType(JSON)
                        .content(registrazione("giusta@test.it", password)))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/auth/login").contentType(JSON)
                        .content("{\"email\":\"giusta@test.it\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void loginConPasswordOltre72ByteDa400() throws Exception {
        mockMvc.perform(post("/api/auth/login").contentType(JSON)
                        .content("{\"email\":\"admin@test.it\",\"password\":\"" + "€".repeat(30) + "\"}"))
                .andExpect(status().isBadRequest());
    }
}
