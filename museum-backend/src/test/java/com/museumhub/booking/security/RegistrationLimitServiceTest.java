package com.museumhub.booking.security;

import com.museumhub.booking.exception.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Prove sul tetto delle registrazioni: 10 per IP ogni ora. */
class RegistrationLimitServiceTest {

    private static final String IP = "203.0.113.7";

    private OrologioFinto orologio;
    private RegistrationLimitService servizio;

    @BeforeEach
    void prepara() {
        orologio = new OrologioFinto();
        servizio = new RegistrationLimitService(10, Duration.ofMinutes(60), orologio);
    }

    @Test
    void laUndicesimaRegistrazioneDalloStessoIpVieneRespinta() {
        for (int i = 0; i < 10; i++) {
            servizio.consuma(IP);
        }
        ApiException e = assertThrows(ApiException.class, () -> servizio.consuma(IP));
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, e.getStatus());
        assertDoesNotThrow(() -> servizio.consuma("198.51.100.1"));
    }

    @Test
    void dopoUnOraSiPuoTornareARegistrarsi() {
        for (int i = 0; i < 10; i++) {
            servizio.consuma(IP);
        }
        orologio.avanza(Duration.ofMinutes(60));
        assertDoesNotThrow(() -> servizio.consuma(IP));
    }
}
