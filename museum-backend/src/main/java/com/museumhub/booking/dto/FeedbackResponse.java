package com.museumhub.booking.dto;

public record FeedbackResponse(Long id, Long tourId, String tour, String utente, Integer voto, String commento,
                               String data) {
}
