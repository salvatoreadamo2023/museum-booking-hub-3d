package com.museumhub.booking.service;

import com.museumhub.booking.dto.ReservationRequest;
import com.museumhub.booking.dto.ReservationResponse;
import com.museumhub.booking.dto.ReservationUpdateRequest;
import com.museumhub.booking.dto.SeatRequest;
import com.museumhub.booking.dto.SeatResponse;
import com.museumhub.booking.entity.Reservation;
import com.museumhub.booking.entity.ReservationSeat;
import com.museumhub.booking.entity.Ruolo;
import com.museumhub.booking.entity.StatoPrenotazione;
import com.museumhub.booking.entity.Tariffa;
import com.museumhub.booking.entity.Tour;
import com.museumhub.booking.entity.User;
import com.museumhub.booking.exception.ApiException;
import com.museumhub.booking.repository.ReservationRepository;
import com.museumhub.booking.repository.ReservationSeatRepository;
import com.museumhub.booking.repository.TariffaRepository;
import com.museumhub.booking.repository.TourRepository;
import com.museumhub.booking.repository.UserRepository;
import com.museumhub.booking.util.Prezzi;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private static final DateTimeFormatter ORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final String POSTO_OCCUPATO = "Posto già prenotato";
    private static final String POSTI_INSUFFICIENTI = "Posti insufficienti nella fascia selezionata";

    private final ReservationRepository reservationRepository;
    private final ReservationSeatRepository seatRepository;
    private final TourRepository tourRepository;
    private final TariffaRepository tariffaRepository;
    private final UserRepository userRepository;

    /** Le prenotazioni dell'utente che ha fatto la richiesta, dalla visita piu' recente. */
    @Transactional(readOnly = true)
    public List<ReservationResponse> elenca(User utente) {
        boolean admin = utente.getRuolo() == Ruolo.ADMIN;
        LocalDateTime adesso = LocalDateTime.now();
        return reservationRepository.findByUserIdOrderByDataVisitaDescOrarioDesc(utente.getId()).stream()
                .map(r -> toResponse(r, admin, adesso))
                .toList();
    }

    @Transactional
    public ReservationResponse crea(User utente, ReservationRequest richiesta) {
        Tour tour = tourRepository.findById(richiesta.tourId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Itinerario non trovato"));
        LocalTime ora = LocalTime.parse(richiesta.orario());
        verificaFasciaAttiva(tour, ora);

        LocalDateTime adesso = LocalDateTime.now();
        if (!LocalDateTime.of(richiesta.data(), ora).isAfter(adesso)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La visita deve essere in una data e un orario futuri");
        }

        Set<Integer> numeri = new HashSet<>();
        for (SeatRequest p : richiesta.posti()) {
            if (p.numero() > tour.getCapienza()) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "Il posto " + p.numero() + " non esiste: la capienza è " + tour.getCapienza());
            }
            if (!numeri.add(p.numero())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Il posto " + p.numero() + " è indicato più volte");
            }
        }

        List<Integer> occupati = seatRepository.findPostiOccupati(tour.getId(), richiesta.data(), ora);
        if (numeri.stream().anyMatch(occupati::contains)) {
            throw new ApiException(HttpStatus.CONFLICT, POSTO_OCCUPATO);
        }

        Map<Long, Tariffa> tariffe = tariffaRepository.findAll().stream()
                .collect(Collectors.toMap(Tariffa::getId, Function.identity()));

        Reservation prenotazione = Reservation.builder()
                .codice(nuovoCodice())
                .user(userRepository.getReferenceById(utente.getId()))
                .tour(tour)
                .dataVisita(richiesta.data())
                .orario(ora)
                .stato(StatoPrenotazione.CONFERMATA)
                .build();
        for (SeatRequest p : richiesta.posti()) {
            Tariffa tariffa = tariffe.get(p.tariffaId());
            if (tariffa == null) {
                throw new ApiException(HttpStatus.NOT_FOUND, "Tariffa non trovata");
            }
            prenotazione.getPosti().add(nuovoPosto(prenotazione, tour, ora, p.numero(), tariffa));
        }
        return toResponse(salva(prenotazione), false, adesso);
    }

    /**
     * Modifica dell'admin: cambia fascia oraria e/o numero di posti. Se i posti diminuiscono si tolgono
     * gli ultimi; i posti nuovi hanno la tariffa predefinita. I posti gia' prenotati mantengono tariffa e
     * prezzo. 409 se nella fascia non c'e' spazio sufficiente.
     */
    @Transactional
    public ReservationResponse aggiorna(Long id, ReservationUpdateRequest richiesta) {
        Reservation prenotazione = trova(id);
        if (prenotazione.getStato() != StatoPrenotazione.CONFERMATA) {
            throw new ApiException(HttpStatus.CONFLICT, "Prenotazione annullata: non può essere modificata");
        }
        Tour tour = prenotazione.getTour();
        LocalTime nuovaOra = LocalTime.parse(richiesta.orario());
        boolean cambiaOra = !nuovaOra.equals(prenotazione.getOrario());
        LocalDateTime adesso = LocalDateTime.now();
        if (cambiaOra) {
            verificaFasciaAttiva(tour, nuovaOra);
            if (!LocalDateTime.of(prenotazione.getDataVisita(), nuovaOra).isAfter(adesso)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "La nuova fascia oraria deve essere futura");
            }
        }
        int numeroPosti = richiesta.numeroPosti();
        if (numeroPosti > tour.getCapienza()) {
            throw new ApiException(HttpStatus.CONFLICT, POSTI_INSUFFICIENTI);
        }

        List<ReservationSeat> attuali = prenotazione.getPosti().stream()
                .sorted(Comparator.comparing(ReservationSeat::getNumeroPosto))
                .collect(Collectors.toCollection(ArrayList::new));
        Set<Integer> occupatiDaAltri = new HashSet<>(
                seatRepository.findPostiOccupati(tour.getId(), prenotazione.getDataVisita(), nuovaOra));
        if (!cambiaOra) {
            attuali.forEach(s -> occupatiDaAltri.remove(s.getNumeroPosto()));
        }

        int daMantenere = Math.min(numeroPosti, attuali.size());
        List<ReservationSeat> mantenuti = new ArrayList<>(attuali.subList(0, daMantenere));
        for (ReservationSeat tolto : new ArrayList<>(attuali.subList(daMantenere, attuali.size()))) {
            prenotazione.getPosti().remove(tolto);
        }

        Set<Integer> usati = new HashSet<>();
        List<ReservationSeat> daRiassegnare = new ArrayList<>();
        for (ReservationSeat posto : mantenuti) {
            if (!occupatiDaAltri.contains(posto.getNumeroPosto()) && usati.add(posto.getNumeroPosto())) {
                posto.setOrario(nuovaOra);
            } else {
                daRiassegnare.add(posto);
            }
        }
        for (ReservationSeat posto : daRiassegnare) {
            posto.setNumeroPosto(prossimoLibero(tour.getCapienza(), occupatiDaAltri, usati));
            posto.setOrario(nuovaOra);
        }
        int nuovi = numeroPosti - mantenuti.size();
        if (nuovi > 0) {
            Tariffa predefinita = tariffaRepository.findByPredefinitaTrue()
                    .orElseThrow(() -> new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                            "Tariffa predefinita non configurata"));
            for (int i = 0; i < nuovi; i++) {
                int numero = prossimoLibero(tour.getCapienza(), occupatiDaAltri, usati);
                prenotazione.getPosti().add(nuovoPosto(prenotazione, tour, nuovaOra, numero, predefinita));
            }
        }
        prenotazione.setOrario(nuovaOra);
        return toResponse(salva(prenotazione), true, adesso);
    }

    /** Disdetta: libera i posti e segna la prenotazione come annullata. Il visitatore puo' farlo fino a 1 ora prima. */
    @Transactional
    public ReservationResponse annulla(User utente, Long id) {
        boolean admin = utente.getRuolo() == Ruolo.ADMIN;
        Reservation prenotazione = trova(id);
        if (!admin && !prenotazione.getUser().getId().equals(utente.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Non puoi annullare le prenotazioni di altri utenti");
        }
        if (prenotazione.getStato() != StatoPrenotazione.CONFERMATA) {
            throw new ApiException(HttpStatus.CONFLICT, "Prenotazione già annullata");
        }
        LocalDateTime adesso = LocalDateTime.now();
        LocalDateTime visita = LocalDateTime.of(prenotazione.getDataVisita(), prenotazione.getOrario());
        if (!admin && adesso.isAfter(visita.minusHours(1))) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Disdetta non più possibile: mancano meno di 1 ora alla visita");
        }
        prenotazione.getPosti().clear();
        prenotazione.setStato(StatoPrenotazione.ANNULLATA);
        return toResponse(reservationRepository.saveAndFlush(prenotazione), admin, adesso);
    }


    private Reservation trova(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Prenotazione non trovata"));
    }

    private void verificaFasciaAttiva(Tour tour, LocalTime ora) {
        boolean attiva = tour.getSlots().stream()
                .anyMatch(s -> s.getOrario().equals(ora) && Boolean.TRUE.equals(s.getAttivo()));
        if (!attiva) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Fascia oraria non disponibile per questo itinerario");
        }
    }

    private ReservationSeat nuovoPosto(Reservation prenotazione, Tour tour, LocalTime ora, int numero, Tariffa tariffa) {
        return ReservationSeat.builder()
                .reservation(prenotazione)
                .tour(tour)
                .dataVisita(prenotazione.getDataVisita())
                .orario(ora)
                .numeroPosto(numero)
                .tariffa(tariffa)
                .prezzoApplicato(Prezzi.calcola(tour.getPrezzo(), tariffa.getScontoPercentuale()))
                .build();
    }

    /** Il vincolo UNIQUE del database e' la difesa finale contro due prenotazioni simultanee dello stesso posto. */
    private Reservation salva(Reservation prenotazione) {
        try {
            return reservationRepository.saveAndFlush(prenotazione);
        } catch (DataIntegrityViolationException e) {
            throw new ApiException(HttpStatus.CONFLICT, POSTO_OCCUPATO);
        }
    }

    private int prossimoLibero(int capienza, Set<Integer> occupati, Set<Integer> usati) {
        for (int numero = 1; numero <= capienza; numero++) {
            if (!occupati.contains(numero) && usati.add(numero)) {
                return numero;
            }
        }
        throw new ApiException(HttpStatus.CONFLICT, POSTI_INSUFFICIENTI);
    }

    private String nuovoCodice() {
        for (int i = 0; i < 50; i++) {
            String codice = "MBH-" + ThreadLocalRandom.current().nextInt(1000, 10000);
            if (!reservationRepository.existsByCodice(codice)) {
                return codice;
            }
        }
        return "MBH-" + ThreadLocalRandom.current().nextInt(100000, 1000000);
    }

    private ReservationResponse toResponse(Reservation r, boolean adminChiede, LocalDateTime adesso) {
        LocalDateTime visita = LocalDateTime.of(r.getDataVisita(), r.getOrario());
        List<SeatResponse> posti = r.getPosti().stream()
                .sorted(Comparator.comparing(ReservationSeat::getNumeroPosto))
                .map(s -> new SeatResponse(s.getNumeroPosto(), s.getTariffa().getId(),
                        s.getTariffa().getNome(), s.getPrezzoApplicato()))
                .toList();
        BigDecimal totale = posti.stream().map(SeatResponse::prezzo)
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
        boolean confermata = r.getStato() == StatoPrenotazione.CONFERMATA;
        boolean annullabile = confermata && (adminChiede || !adesso.isAfter(visita.minusHours(1)));
        return new ReservationResponse(r.getId(), r.getCodice(), r.getTour().getId(), r.getTour().getTitolo(),
                r.getDataVisita().toString(), r.getOrario().format(ORA), posti, totale, r.getStato(),
                r.getUser().getId(), r.getUser().getNome(), r.getUser().getEmail(),
                annullabile, visita.isBefore(adesso));
    }
}
