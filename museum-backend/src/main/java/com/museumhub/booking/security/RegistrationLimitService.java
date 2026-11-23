package com.museumhub.booking.security;

import com.museumhub.booking.exception.ApiException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * Tetto alle registrazioni dallo stesso indirizzo IP: la registrazione e' pubblica e crea subito utente e
 * sessione, quindi senza limite uno script potrebbe riempire il database di account finti.
 * Conta ogni tentativo che arriva al servizio, anche quelli respinti con 409 (email gia' usata): cosi' non si
 * puo' nemmeno sondare all'infinito quali email sono registrate.
 */
@Service
public class RegistrationLimitService {

    private final Clock orologio;
    private final int massimo;
    private final LimitatoreAFinestra perIp;

    @Autowired
    public RegistrationLimitService(@Value("${app.security.register-max-per-ip:20}") int massimo,
                                    @Value("${app.security.register-window-minutes:60}") long minuti) {
        this(massimo, Duration.ofMinutes(minuti), Clock.systemUTC());
    }

    RegistrationLimitService(int massimo, Duration finestra, Clock orologio) {
        this.orologio = orologio;
        this.massimo = massimo;
        this.perIp = new LimitatoreAFinestra(LoginAttemptService.MASSIMO_CHIAVI, finestra);
    }

    /** Registra il tentativo; lancia 429 se l'IP ha gia' raggiunto il tetto nella finestra in corso. */
    public void consuma(String indirizzo) {
        String ip = IndirizzoRete.chiave(indirizzo);
        Instant ora = orologio.instant();
        if (!perIp.consumaSeSotto(ip, ora, massimo)) { // controllo e conteggio atomici
            long minuti = perIp.minutiAllaFine(ip, ora);
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                    "Troppe registrazioni da questa rete: riprova tra " + minuti + (minuti == 1 ? " minuto" : " minuti"));
        }
    }
}
