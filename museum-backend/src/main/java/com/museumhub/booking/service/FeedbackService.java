package com.museumhub.booking.service;

import com.museumhub.booking.dto.FeedbackRequest;
import com.museumhub.booking.dto.FeedbackResponse;
import com.museumhub.booking.dto.PaginaResponse;
import com.museumhub.booking.entity.Feedback;
import com.museumhub.booking.entity.Tour;
import com.museumhub.booking.entity.User;
import com.museumhub.booking.exception.ApiException;
import com.museumhub.booking.repository.FeedbackRepository;
import com.museumhub.booking.repository.ReservationRepository;
import com.museumhub.booking.repository.TourRepository;
import com.museumhub.booking.repository.UserRepository;
import com.museumhub.booking.util.Paginazione;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;

@Service
@RequiredArgsConstructor
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final ReservationRepository reservationRepository;
    private final TourRepository tourRepository;
    private final UserRepository userRepository;

    /**
     * Un visitatore puo' lasciare un feedback per un itinerario solo dopo una visita confermata
     * gia' avvenuta, e al massimo uno per ogni visita.
     */
    @Transactional
    public FeedbackResponse crea(User utente, FeedbackRequest r) {
        Tour tour = tourRepository.findById(r.tourId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Itinerario non trovato"));
        long visite = reservationRepository.contaVisiteConcluse(
                utente.getId(), tour.getId(), LocalDate.now(), LocalTime.now());
        if (visite == 0) {
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "Puoi lasciare un feedback solo dopo aver completato una visita");
        }
        if (feedbackRepository.countByUserIdAndTourId(utente.getId(), tour.getId()) >= visite) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Hai già lasciato un feedback per ogni visita completata di questo itinerario");
        }
        Feedback feedback = feedbackRepository.save(Feedback.builder()
                .user(userRepository.getReferenceById(utente.getId()))
                .tour(tour)
                .voto(r.voto())
                .commento(r.commento().trim())
                .build());
        return toResponse(feedback, utente.getNome());
    }

    @Transactional(readOnly = true)
    public PaginaResponse<FeedbackResponse> elenca(int pagina, int dimensione) {
        // dal piu' recente; l'id nell'ordine tiene stabili le pagine anche a parita' di data
        Sort ordine = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
        return PaginaResponse.di(feedbackRepository.findAllBy(Paginazione.richiesta(pagina, dimensione, ordine)),
                f -> toResponse(f, f.getUser().getNome()));
    }

    private FeedbackResponse toResponse(Feedback f, String nomeUtente) {
        String data = f.getCreatedAt() != null ? f.getCreatedAt().toLocalDate().toString() : LocalDate.now().toString();
        return new FeedbackResponse(f.getId(), f.getTour().getId(), f.getTour().getTitolo(), nomeUtente,
                f.getVoto(), f.getCommento(), data);
    }
}
