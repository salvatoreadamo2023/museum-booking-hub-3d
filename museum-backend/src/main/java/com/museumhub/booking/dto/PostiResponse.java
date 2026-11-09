package com.museumhub.booking.dto;

import java.util.List;

/** Posti di una fascia oraria: quali numeri sono occupati e quanti ne restano liberi. */
public record PostiResponse(Long tourId, String data, String orario, int capienza, List<Integer> occupati, int liberi) {
}
