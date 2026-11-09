package com.museumhub.booking.service;

import com.museumhub.booking.dto.PostiResponse;
import com.museumhub.booking.dto.SlotResponse;
import com.museumhub.booking.dto.TariffaResponse;
import com.museumhub.booking.dto.TourRequest;
import com.museumhub.booking.dto.TourResponse;
import com.museumhub.booking.entity.Tariffa;
import com.museumhub.booking.entity.Tour;
import com.museumhub.booking.entity.TourSlot;
import com.museumhub.booking.exception.ApiException;
import com.museumhub.booking.repository.ReservationRepository;
import com.museumhub.booking.repository.ReservationSeatRepository;
import com.museumhub.booking.repository.TariffaRepository;
import com.museumhub.booking.repository.TourRepository;
import com.museumhub.booking.util.Prezzi;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TourService {

    private static final DateTimeFormatter ORA = DateTimeFormatter.ofPattern("HH:mm");

    private final TourRepository tourRepository;
    private final TariffaRepository tariffaRepository;
    private final ReservationRepository reservationRepository;
    private final ReservationSeatRepository seatRepository;

    /** Elenco degli itinerari con i posti liberi di ogni fascia nella data indicata (oggi se omessa). */
    @Transactional(readOnly = true)
    public List<TourResponse> elenca(LocalDate data) {
        LocalDate giorno = data != null ? data : LocalDate.now();
        List<Tariffa> tariffe = tariffe();
        return tourRepository.findAll(Sort.by("id")).stream()
                .map(t -> toResponse(t, giorno, tariffe))
                .toList();
    }

    @Transactional
    public TourResponse crea(TourRequest r) {
        Tour tour = Tour.builder()
                .titolo(r.titolo().trim())
                .descrizione(r.descrizione())
                .durataMin(r.durataMin())
                .prezzo(r.prezzo())
                .capienza(r.capienza())
                .build();
        sincronizzaOrari(tour, r.orari());
        tour = tourRepository.save(tour);
        return toResponse(tour, LocalDate.now(), tariffe());
    }

    @Transactional
    public TourResponse aggiorna(Long id, TourRequest r) {
        Tour tour = trova(id);
        Integer maxPosto = seatRepository.maxPostoPrenotato(id, LocalDate.now());
        if (maxPosto != null && r.capienza() < maxPosto) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Capienza troppo bassa: esistono prenotazioni future fino al posto " + maxPosto);
        }
        tour.setTitolo(r.titolo().trim());
        tour.setDescrizione(r.descrizione());
        tour.setDurataMin(r.durataMin());
        tour.setPrezzo(r.prezzo());
        tour.setCapienza(r.capienza());
        sincronizzaOrari(tour, r.orari());
        return toResponse(tour, LocalDate.now(), tariffe());
    }

    @Transactional
    public void elimina(Long id) {
        trova(id);
        if (reservationRepository.existsByTourId(id)) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Itinerario con prenotazioni: non può essere eliminato");
        }
        tourRepository.deleteById(id);
    }

    /** Posti occupati di una fascia: la mappa 3D li usa per colorare i posti gia' presi. */
    @Transactional(readOnly = true)
    public PostiResponse posti(Long id, LocalDate data, String orario) {
        Tour tour = trova(id);
        LocalTime ora;
        try {
            ora = LocalTime.parse(orario);
        } catch (DateTimeParseException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Orario non valido (formato HH:mm)");
        }
        boolean esiste = tour.getSlots().stream().anyMatch(s -> s.getOrario().equals(ora));
        if (!esiste) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Fascia oraria non trovata per questo itinerario");
        }
        List<Integer> occupati = seatRepository.findPostiOccupati(id, data, ora);
        return new PostiResponse(id, data.toString(), ora.format(ORA), tour.getCapienza(),
                occupati, tour.getCapienza() - occupati.size());
    }

    private Tour trova(Long id) {
        return tourRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Itinerario non trovato"));
    }

    private List<Tariffa> tariffe() {
        return tariffaRepository.findAll(Sort.by("id"));
    }

    /**
     * Aggiorna le fasce orarie con quelle ricevute: aggiunge le nuove e toglie quelle mancanti,
     * ma una fascia con prenotazioni future non si puo' togliere.
     */
    private void sincronizzaOrari(Tour tour, List<String> orari) {
        Set<LocalTime> nuovi = orari.stream().map(LocalTime::parse)
                .collect(Collectors.toCollection(TreeSet::new));

        Iterator<TourSlot> it = tour.getSlots().iterator();
        while (it.hasNext()) {
            TourSlot slot = it.next();
            if (!nuovi.contains(slot.getOrario())) {
                if (tour.getId() != null && seatRepository.existsByTourIdAndOrarioAndDataVisitaGreaterThanEqual(
                        tour.getId(), slot.getOrario(), LocalDate.now())) {
                    throw new ApiException(HttpStatus.CONFLICT,
                            "Impossibile rimuovere la fascia " + slot.getOrario().format(ORA)
                                    + ": ci sono prenotazioni future");
                }
                it.remove();
            }
        }
        Set<LocalTime> esistenti = tour.getSlots().stream().map(TourSlot::getOrario)
                .collect(Collectors.toSet());
        for (LocalTime ora : nuovi) {
            if (!esistenti.contains(ora)) {
                tour.getSlots().add(TourSlot.builder().tour(tour).orario(ora).attivo(true).build());
            }
        }
    }

    private TourResponse toResponse(Tour tour, LocalDate giorno, List<Tariffa> tariffe) {
        Map<LocalTime, Long> occupati = new HashMap<>();
        for (Object[] riga : seatRepository.contaPostiPerOrario(tour.getId(), giorno)) {
            occupati.put((LocalTime) riga[0], (Long) riga[1]);
        }
        List<SlotResponse> slots = tour.getSlots().stream()
                .sorted(Comparator.comparing(TourSlot::getOrario))
                .map(s -> new SlotResponse(
                        s.getOrario().format(ORA),
                        Math.max(0, tour.getCapienza() - occupati.getOrDefault(s.getOrario(), 0L).intValue()),
                        Boolean.TRUE.equals(s.getAttivo())))
                .toList();
        List<TariffaResponse> prezzi = tariffe.stream()
                .map(t -> new TariffaResponse(t.getId(), t.getNome(), t.getScontoPercentuale(),
                        Prezzi.calcola(tour.getPrezzo(), t.getScontoPercentuale()),
                        Boolean.TRUE.equals(t.getPredefinita())))
                .toList();
        return new TourResponse(tour.getId(), tour.getTitolo(), tour.getDescrizione(), tour.getDurataMin(),
                tour.getPrezzo(), tour.getCapienza(), slots, prezzi);
    }
}
