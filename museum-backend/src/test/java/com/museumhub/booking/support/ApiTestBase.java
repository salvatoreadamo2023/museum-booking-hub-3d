package com.museumhub.booking.support;

import com.jayway.jsonpath.JsonPath;
import com.museumhub.booking.entity.Reservation;
import com.museumhub.booking.entity.ReservationSeat;
import com.museumhub.booking.entity.Tariffa;
import com.museumhub.booking.entity.Tour;
import com.museumhub.booking.entity.User;
import com.museumhub.booking.repository.ReservationRepository;
import com.museumhub.booking.repository.TariffaRepository;
import com.museumhub.booking.repository.TourRepository;
import com.museumhub.booking.repository.UserRepository;
import com.museumhub.booking.util.Prezzi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.concurrent.ThreadLocalRandom;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** Base comune dei test funzionali: database H2, richieste MockMvc e dati di prova annullati a fine test. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public abstract class ApiTestBase {

    protected static final String AUTH = "Authorization";
    protected static final MediaType JSON = MediaType.APPLICATION_JSON;

    private static final String TOUR = """
            {"titolo":"Tour Test","descrizione":"Percorso di prova","durataMin":60,
             "prezzo":12.00,"capienza":10,"orari":["09:30","11:00"]}""";

    @Autowired
    protected MockMvc mockMvc;
    @Autowired
    protected UserRepository userRepository;
    @Autowired
    protected TourRepository tourRepository;
    @Autowired
    protected TariffaRepository tariffaRepository;
    @Autowired
    protected ReservationRepository reservationRepository;

    protected String bearer(String token) {
        return "Bearer " + token;
    }

    /** Esegue il login e restituisce la risposta completa (corpo JSON e cookie). */
    private MockHttpServletResponse login(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/login").contentType(JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andReturn().getResponse();
    }

    /** Valore di un cookie impostato dalla risposta (header Set-Cookie), o null se assente. */
    protected String cookie(MockHttpServletResponse risposta, String nome) {
        for (String header : risposta.getHeaders("Set-Cookie")) {
            if (header.startsWith(nome + "=")) {
                int fine = header.indexOf(';');
                return header.substring(nome.length() + 1, fine < 0 ? header.length() : fine);
            }
        }
        return null;
    }

    protected String tokenAdmin() throws Exception {
        return cookie(login("admin@test.it", "PasswordAdmin1"), "mbh_access");
    }

    protected int idAdmin() throws Exception {
        return JsonPath.read(login("admin@test.it", "PasswordAdmin1").getContentAsString(), "$.id");
    }

    /** Registra un visitatore (password "password123") e restituisce la risposta completa. */
    protected MockHttpServletResponse registraRisposta(String email) throws Exception {
        return mockMvc.perform(post("/api/auth/register").contentType(JSON)
                        .content("{\"nome\":\"Visitatore " + email + "\",\"email\":\"" + email
                                + "\",\"password\":\"password123\"}"))
                .andReturn().getResponse();
    }

    /** Registra un visitatore e restituisce il corpo JSON della risposta (con l'id). */
    protected String registraCorpo(String email) throws Exception {
        return registraRisposta(email).getContentAsString();
    }

    /** Registra un visitatore e restituisce il suo token di accesso (preso dal cookie). */
    protected String registra(String email) throws Exception {
        return cookie(registraRisposta(email), "mbh_access");
    }

    /** Crea l'itinerario di prova (capienza 10, fasce 09:30 e 11:00) e ne restituisce l'id. */
    protected int creaTour() throws Exception {
        String body = mockMvc.perform(post("/api/tours").header(AUTH, bearer(tokenAdmin()))
                        .contentType(JSON).content(TOUR))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    protected long tariffaId(String nome) {
        return tariffaRepository.findAll().stream()
                .filter(t -> t.getNome().equals(nome)).findFirst().orElseThrow().getId();
    }

    protected String posto(int numero, long tariffaId) {
        return "{\"numero\":" + numero + ",\"tariffaId\":" + tariffaId + "}";
    }

    /** Posti da "da" ad "a" tutti con la stessa tariffa, come elenco JSON. */
    protected String postiDa(int da, int a, long tariffaId) {
        StringBuilder sb = new StringBuilder();
        for (int n = da; n <= a; n++) {
            if (sb.length() > 0) {
                sb.append(",");
            }
            sb.append(posto(n, tariffaId));
        }
        return sb.toString();
    }

    protected String bodyPrenotazione(int tourId, LocalDate data, String orario, String... posti) {
        return "{\"tourId\":" + tourId + ",\"data\":\"" + data + "\",\"orario\":\"" + orario
                + "\",\"posti\":[" + String.join(",", posti) + "]}";
    }

    protected ResultActions prenota(String token, String body) throws Exception {
        return mockMvc.perform(post("/api/reservations").header(AUTH, bearer(token))
                .contentType(JSON).content(body));
    }

    /** Inserisce direttamente una prenotazione confermata (anche nel passato, che l'API non consente). */
    protected Reservation inserisciPrenotazione(String emailVisitatore, int tourId, LocalDate data, String orario,
                                                int... numeri) {
        User user = userRepository.findByEmail(emailVisitatore).orElseThrow();
        Tour tour = tourRepository.findById((long) tourId).orElseThrow();
        Tariffa intero = tariffaRepository.findByPredefinitaTrue().orElseThrow();
        LocalTime ora = LocalTime.parse(orario);
        Reservation r = Reservation.builder()
                .codice("MBH-" + ThreadLocalRandom.current().nextInt(100000, 999999))
                .user(user).tour(tour).dataVisita(data).orario(ora).build();
        for (int numero : numeri) {
            r.getPosti().add(ReservationSeat.builder().reservation(r).tour(tour).dataVisita(data).orario(ora)
                    .numeroPosto(numero).tariffa(intero)
                    .prezzoApplicato(Prezzi.calcola(tour.getPrezzo(), intero.getScontoPercentuale())).build());
        }
        return reservationRepository.saveAndFlush(r);
    }
}
