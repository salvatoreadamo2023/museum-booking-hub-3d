package com.museumhub.booking.security;

import com.museumhub.booking.exception.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Prove sul limitatore dei login, senza Spring: 5 errori per coppia IP + email, 20 per IP, finestra 15 minuti. */
class LoginAttemptServiceTest {

    private static final String IP = "203.0.113.7";

    private OrologioFinto orologio;
    private LoginAttemptService servizio;

    @BeforeEach
    void prepara() {
        orologio = new OrologioFinto();
        servizio = new LoginAttemptService(5, 20, Duration.ofMinutes(15), orologio);
    }

    private void sbaglia(String ip, String email, int volte) {
        for (int i = 0; i < volte; i++) {
            servizio.verifica(ip, email);
            servizio.fallito(ip, email);
        }
    }

    @Test
    void dopoCinqueErroriLaCoppiaEBloccataMaNonLaStessaEmailDaUnAltroIp() {
        sbaglia(IP, "mario@test.it", 5);
        ApiException e = assertThrows(ApiException.class, () -> servizio.verifica(IP, "mario@test.it"));
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, e.getStatus());
        assertDoesNotThrow(() -> servizio.verifica("198.51.100.1", "mario@test.it"));
    }

    @Test
    void lIpCheProvaTanteEmailDiverseVieneFermatoDalTettoPerIp() {
        for (int i = 0; i < 20; i++) {
            sbaglia(IP, "utente" + i + "@test.it", 1); // ogni email resta sotto la soglia della coppia
        }
        assertThrows(ApiException.class, () -> servizio.verifica(IP, "nuova@test.it"));
        // un altro IP non ne risente
        assertDoesNotThrow(() -> servizio.verifica("198.51.100.1", "nuova@test.it"));
    }

    @Test
    void ilBloccoFinisceAlloScadereDellaFinestra() {
        sbaglia(IP, "mario@test.it", 5);
        orologio.avanza(Duration.ofMinutes(14));
        ApiException e = assertThrows(ApiException.class, () -> servizio.verifica(IP, "mario@test.it"));
        assertTrue(e.getMessage().contains("1 minuto"), e.getMessage());
        orologio.avanza(Duration.ofMinutes(1));
        assertDoesNotThrow(() -> servizio.verifica(IP, "mario@test.it"));
    }

    @Test
    void unAccessoRiuscitoNonAzzeraIlContatoreDellIp() {
        for (int i = 0; i < 19; i++) {
            sbaglia(IP, "vittima" + i + "@test.it", 1);
        }
        servizio.riuscito(IP, "mioaccount@test.it");
        sbaglia(IP, "altra@test.it", 1);
        assertThrows(ApiException.class, () -> servizio.verifica(IP, "mioaccount@test.it"));
    }

    @Test
    void gliIndirizziIpv6DellaStessaReteContanoComeUnoSolo() {
        // stesso /64, ultimi 64 bit diversi a ogni tentativo: non deve bastare per aggirare il tetto
        for (int i = 0; i < 20; i++) {
            sbaglia("2001:db8:1:2::" + Integer.toHexString(i + 1), "utente" + i + "@test.it", 1);
        }
        assertThrows(ApiException.class, () -> servizio.verifica("2001:db8:1:2:abcd::99", "nuova@test.it"));
        assertDoesNotThrow(() -> servizio.verifica("2001:db8:1:3::1", "nuova@test.it"));
        assertEquals("1.2.3.4", IndirizzoRete.chiave("::ffff:1.2.3.4"));
    }

    @Test
    void laMemoriaNonCresceOltreIlLimiteAncheConMilioniDiChiavi() {
        // 50.000 IP ed email tutti diversi: prima della correzione la mappa li teneva tutti per 15 minuti
        for (int i = 0; i < 50_000; i++) {
            servizio.fallito("10." + (i / 65536) + "." + (i / 256 % 256) + "." + (i % 256), "spam" + i + "@test.it");
        }
        assertTrue(servizio.chiaviInMemoria() <= 2 * LoginAttemptService.MASSIMO_CHIAVI,
                "chiavi in memoria: " + servizio.chiaviInMemoria());
    }
}
