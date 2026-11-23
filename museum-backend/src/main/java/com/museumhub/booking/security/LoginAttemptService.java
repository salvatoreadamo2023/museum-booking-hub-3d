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
 * Limita i tentativi di accesso errati (difesa dalla forza bruta) su due livelli:
 * <ul>
 *   <li>per coppia indirizzo IP + email: dopo N errori quell'account da quell'IP resta bloccato fino alla fine
 *       della finestra, anche con la password giusta;</li>
 *   <li>per solo indirizzo IP: un tetto agli errori totali, cosi' non si possono provare migliaia di email
 *       diverse dallo stesso IP (ognuna sotto la soglia della coppia) per riempire la memoria.</li>
 * </ul>
 * Le richieste bloccate vengono respinte prima di toccare il database e BCrypt, quindi costano pochissimo.
 * Limiti noti: lo stato e' in memoria e vale per la singola istanza; dietro un reverse proxy l'IP visto e'
 * quello del proxy, quindi in produzione il proxy deve passare l'IP reale o fare lui il rate limit.
 */
@Service
public class LoginAttemptService {

    static final int MASSIMO_CHIAVI = 10_000;

    private final Clock orologio;
    private final int massimoPerAccount;
    private final int massimoPerIp;
    private final LimitatoreAFinestra perAccount;
    private final LimitatoreAFinestra perIp;

    @Autowired
    public LoginAttemptService(@Value("${app.security.login-max-attempts:5}") int massimoPerAccount,
                               @Value("${app.security.login-max-failures-per-ip:30}") int massimoPerIp,
                               @Value("${app.security.login-lock-minutes:15}") long minuti) {
        this(massimoPerAccount, massimoPerIp, Duration.ofMinutes(minuti), Clock.systemUTC());
    }

    LoginAttemptService(int massimoPerAccount, int massimoPerIp, Duration durata, Clock orologio) {
        this.orologio = orologio;
        this.massimoPerAccount = massimoPerAccount;
        this.massimoPerIp = massimoPerIp;
        this.perAccount = new LimitatoreAFinestra(MASSIMO_CHIAVI, durata);
        this.perIp = new LimitatoreAFinestra(MASSIMO_CHIAVI, durata);
    }

    /** Lancia 429 se l'IP o la coppia IP + email hanno esaurito i tentativi. */
    public void verifica(String indirizzo, String email) {
        String ip = IndirizzoRete.chiave(indirizzo);
        Instant ora = orologio.instant();
        if (perIp.conteggio(ip, ora) >= massimoPerIp) {
            throw troppi(perIp.minutiAllaFine(ip, ora));
        }
        String coppia = coppia(ip, email);
        if (perAccount.conteggio(coppia, ora) >= massimoPerAccount) {
            throw troppi(perAccount.minutiAllaFine(coppia, ora));
        }
    }

    public void fallito(String indirizzo, String email) {
        String ip = IndirizzoRete.chiave(indirizzo);
        Instant ora = orologio.instant();
        perIp.incrementa(ip, ora);
        perAccount.incrementa(coppia(ip, email), ora);
    }

    /**
     * Un accesso riuscito azzera solo il contatore della coppia. Il contatore dell'IP resta: altrimenti
     * basterebbe entrare ogni tanto con il proprio account per ripartire da zero con i tentativi.
     */
    public void riuscito(String indirizzo, String email) {
        String ip = IndirizzoRete.chiave(indirizzo);
        perAccount.azzera(coppia(ip, email));
    }

    int chiaviInMemoria() {
        return perAccount.dimensione() + perIp.dimensione();
    }

    private static String coppia(String ip, String email) {
        return ip + "|" + email;
    }

    private static ApiException troppi(long minuti) {
        return new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                "Troppi tentativi di accesso: riprova tra " + minuti + (minuti == 1 ? " minuto" : " minuti"));
    }
}
