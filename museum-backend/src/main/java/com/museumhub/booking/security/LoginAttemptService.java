package com.museumhub.booking.security;

import com.museumhub.booking.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limita i tentativi di accesso errati (difesa dalla forza bruta). Dopo N errori consecutivi per la stessa
 * coppia indirizzo IP + email l'accesso viene bloccato per qualche minuto, anche con la password giusta.
 * Limite noto: lo stato e' in memoria, quindi vale per la singola istanza del backend; con piu' istanze
 * andrebbe spostato in una risorsa condivisa (database o Redis).
 */
@Service
public class LoginAttemptService {

    private static final int DIMENSIONE_MASSIMA = 10_000;

    private static final class Tentativi {
        private int falliti;
        private Instant primoFallimento;
        private Instant bloccatoFinoA;
    }

    private final Map<String, Tentativi> tentativi = new ConcurrentHashMap<>();
    private final int massimo;
    private final Duration durata;

    public LoginAttemptService(@Value("${app.security.login-max-attempts:5}") int massimo,
                               @Value("${app.security.login-lock-minutes:15}") long minuti) {
        this.massimo = massimo;
        this.durata = Duration.ofMinutes(minuti);
    }

    /** Lancia 429 se la chiave e' bloccata. */
    public void verifica(String chiave) {
        Tentativi t = tentativi.get(chiave);
        if (t == null || t.bloccatoFinoA == null) {
            return;
        }
        Instant ora = Instant.now();
        if (ora.isBefore(t.bloccatoFinoA)) {
            long minuti = Duration.between(ora, t.bloccatoFinoA).toMinutes() + 1;
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                    "Troppi tentativi di accesso: riprova tra " + minuti + " minuti");
        }
        tentativi.remove(chiave);
    }

    public void fallito(String chiave) {
        Instant ora = Instant.now();
        if (tentativi.size() > DIMENSIONE_MASSIMA) {
            tentativi.values().removeIf(t -> scaduto(t, ora));
        }
        tentativi.compute(chiave, (k, t) -> {
            if (t == null || scaduto(t, ora)) {
                t = new Tentativi();
                t.primoFallimento = ora;
            }
            t.falliti++;
            if (t.falliti >= massimo) {
                t.bloccatoFinoA = ora.plus(durata);
            }
            return t;
        });
    }

    public void riuscito(String chiave) {
        tentativi.remove(chiave);
    }

    private boolean scaduto(Tentativi t, Instant ora) {
        if (t.bloccatoFinoA != null) {
            return ora.isAfter(t.bloccatoFinoA);
        }
        return ora.isAfter(t.primoFallimento.plus(durata));
    }
}
