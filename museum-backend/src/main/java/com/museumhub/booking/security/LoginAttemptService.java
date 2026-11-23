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

    /**
     * Prenota un tentativo prima di controllare la password: lo conta subito e lancia 429 se l'IP o la coppia
     * IP + email hanno esaurito i tentativi. Contare prima e non dopo l'errore chiude la corsa delle richieste
     * in parallelo: cento login simultanei non possono piu' passare tutti il controllo prima che arrivi il
     * primo errore. Se poi la password e' giusta, {@link #riuscito} restituisce il tentativo.
     */
    public void prenota(String indirizzo, String email) {
        String ip = IndirizzoRete.chiave(indirizzo);
        Instant ora = orologio.instant();
        if (!perIp.consumaSeSotto(ip, ora, massimoPerIp)) {
            throw troppi(perIp.minutiAllaFine(ip, ora));
        }
        String coppia = coppia(ip, email);
        if (!perAccount.consumaSeSotto(coppia, ora, massimoPerAccount)) {
            perIp.restituisci(ip, ora); // il tentativo non e' stato fatto: non va contato all'IP
            throw troppi(perAccount.minutiAllaFine(coppia, ora));
        }
    }

    /**
     * Password giusta: la coppia riparte da zero e all'IP torna il tentativo prenotato. Gli errori gia' fatti
     * dall'IP restano: altrimenti basterebbe entrare ogni tanto con il proprio account per azzerarli.
     */
    public void riuscito(String indirizzo, String email) {
        String ip = IndirizzoRete.chiave(indirizzo);
        perAccount.azzera(coppia(ip, email));
        perIp.restituisci(ip, orologio.instant());
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
