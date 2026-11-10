package com.museumhub.booking.service;

import com.museumhub.booking.dto.StatsResponse;
import com.museumhub.booking.dto.StatsResponse.Giorno;
import com.museumhub.booking.dto.StatsResponse.QuotaItinerario;
import com.museumhub.booking.entity.Tour;
import com.museumhub.booking.exception.ApiException;
import com.museumhub.booking.repository.ReservationSeatRepository;
import com.museumhub.booking.repository.TourRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Statistiche calcolate dai posti prenotati (le prenotazioni annullate non hanno posti, quindi non contano).
 * Periodi: "oggi", "7giorni" (oggi e i 6 giorni precedenti), "mese" (gli ultimi 30 giorni).
 */
@Service
@RequiredArgsConstructor
public class StatsService {

    private static final DateTimeFormatter ORA = DateTimeFormatter.ofPattern("HH:mm");

    private final ReservationSeatRepository seatRepository;
    private final TourRepository tourRepository;

    /** Totali di un intervallo di date. */
    private record Totali(BigDecimal incasso, int biglietti, Map<LocalDate, BigDecimal> incassoPerGiorno,
                          Map<LocalDate, Integer> bigliettiPerGiorno, Map<LocalTime, Integer> bigliettiPerOra,
                          Map<Long, Integer> bigliettiPerTour, Map<Long, BigDecimal> incassoPerTour) {
    }

    @Transactional(readOnly = true)
    public StatsResponse calcola(String periodo) {
        int giorni = switch (periodo) {
            case "oggi" -> 1;
            case "7giorni" -> 7;
            case "mese" -> 30;
            default -> throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Periodo non valido: usa oggi, 7giorni oppure mese");
        };
        LocalDate fine = LocalDate.now();
        LocalDate inizio = fine.minusDays(giorni - 1L);
        LocalDate inizioPrec = inizio.minusDays(giorni);
        LocalDate finePrec = inizio.minusDays(1);

        List<Tour> tours = tourRepository.findAll();
        int postiGiornalieri = tours.stream()
                .mapToInt(t -> (int) t.getSlots().stream().filter(s -> Boolean.TRUE.equals(s.getAttivo())).count()
                        * t.getCapienza())
                .sum();
        int postiTotali = postiGiornalieri * giorni;

        Totali corrente = aggrega(inizio, fine);
        Totali precedente = aggrega(inizioPrec, finePrec);

        double percentuale = percentuale(corrente.biglietti(), postiTotali);
        double percentualePrec = percentuale(precedente.biglietti(), postiTotali);

        List<Giorno> andamento = new ArrayList<>();
        for (LocalDate g = inizio; !g.isAfter(fine); g = g.plusDays(1)) {
            andamento.add(new Giorno(g.toString(),
                    corrente.incassoPerGiorno().getOrDefault(g, BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP),
                    corrente.bigliettiPerGiorno().getOrDefault(g, 0)));
        }

        List<QuotaItinerario> quote = tours.stream()
                .map(t -> {
                    int biglietti = corrente.bigliettiPerTour().getOrDefault(t.getId(), 0);
                    return new QuotaItinerario(t.getId(), t.getTitolo(), biglietti,
                            corrente.incassoPerTour().getOrDefault(t.getId(), BigDecimal.ZERO)
                                    .setScale(2, RoundingMode.HALF_UP),
                            percentuale(biglietti, corrente.biglietti()));
                })
                .sorted(Comparator.comparingInt(QuotaItinerario::biglietti).reversed())
                .toList();

        String oraDiPunta = null;
        int massimo = 0;
        for (Map.Entry<LocalTime, Integer> e : corrente.bigliettiPerOra().entrySet()) {
            if (e.getValue() >= massimo) { // a parita' vince l'orario piu' tardi (la mappa e' ordinata)
                massimo = e.getValue();
                oraDiPunta = e.getKey().format(ORA);
            }
        }

        return new StatsResponse(periodo, inizio.toString(), fine.toString(),
                corrente.incasso().setScale(2, RoundingMode.HALF_UP),
                variazione(corrente.incasso(), precedente.incasso()),
                corrente.biglietti(),
                variazione(BigDecimal.valueOf(corrente.biglietti()), BigDecimal.valueOf(precedente.biglietti())),
                oraDiPunta,
                corrente.biglietti(), postiTotali, percentuale,
                arrotonda(percentuale - percentualePrec),
                andamento, quote);
    }

    private Totali aggrega(LocalDate da, LocalDate a) {
        BigDecimal incasso = BigDecimal.ZERO;
        int biglietti = 0;
        Map<LocalDate, BigDecimal> incassoGiorno = new TreeMap<>();
        Map<LocalDate, Integer> bigliettiGiorno = new TreeMap<>();
        Map<LocalTime, Integer> bigliettiOra = new TreeMap<>();
        Map<Long, Integer> bigliettiTour = new HashMap<>();
        Map<Long, BigDecimal> incassoTour = new HashMap<>();

        for (Object[] riga : seatRepository.righeStatistiche(da, a)) {
            LocalDate giorno = (LocalDate) riga[0];
            LocalTime ora = (LocalTime) riga[1];
            Long tourId = (Long) riga[2];
            BigDecimal prezzo = (BigDecimal) riga[3];
            incasso = incasso.add(prezzo);
            biglietti++;
            incassoGiorno.merge(giorno, prezzo, BigDecimal::add);
            bigliettiGiorno.merge(giorno, 1, Integer::sum);
            bigliettiOra.merge(ora, 1, Integer::sum);
            bigliettiTour.merge(tourId, 1, Integer::sum);
            incassoTour.merge(tourId, prezzo, BigDecimal::add);
        }
        return new Totali(incasso, biglietti, incassoGiorno, bigliettiGiorno, bigliettiOra, bigliettiTour, incassoTour);
    }

    private Double variazione(BigDecimal corrente, BigDecimal precedente) {
        if (precedente.signum() == 0) {
            return null;
        }
        return arrotonda(corrente.subtract(precedente).multiply(BigDecimal.valueOf(100))
                .divide(precedente, 4, RoundingMode.HALF_UP).doubleValue());
    }

    private double percentuale(int parte, int totale) {
        return totale == 0 ? 0.0 : arrotonda(parte * 100.0 / totale);
    }

    private double arrotonda(double valore) {
        return Math.round(valore * 10.0) / 10.0;
    }
}
